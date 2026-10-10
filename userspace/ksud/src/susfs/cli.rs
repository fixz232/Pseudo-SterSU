//! CLI facade for the SUSFS ABI. The accepted command names also match the
//! existing `ksu_susfs` tool so the Manager can use this when that tool is lost.

use anyhow::{Context, Result, bail, ensure};

use super::cmd::{kstat, paths, spoof, status};

fn expect_len(args: &[String], expected: usize) -> Result<()> {
    ensure!(
        args.len() == expected,
        "SUSFS command expected {} argument(s), got {}",
        expected - 1,
        args.len().saturating_sub(1)
    );
    Ok(())
}

fn enabled(value: &str) -> Result<bool> {
    match value {
        "0" => Ok(false),
        "1" => Ok(true),
        _ => bail!("SUSFS toggle must be 0 or 1"),
    }
}

pub fn run(args: &[String]) -> Result<()> {
    let Some(command) = args.first() else {
        bail!("SUSFS command is required");
    };
    let command = command.replace('-', "_");
    if command == "show" {
        expect_len(args, 2)?;
    }
    let query = if command == "show" {
        args[1].as_str()
    } else {
        command.as_str()
    };
    match query {
        "status" | "version" | "variant" | "enabled_features" | "features"
            if command == "show" || args.len() == 1 =>
        {
            match query {
                "status" => println!("{}", status::get_susfs_status()),
                "version" => {
                    let version = status::get_susfs_version();
                    ensure!(version != "unsupport", "SUSFS kernel interface unavailable");
                    println!("{version}");
                }
                "variant" => {
                    let variant = status::get_susfs_variant();
                    ensure!(!variant.is_empty(), "SUSFS variant unavailable");
                    println!("{variant}");
                }
                _ => {
                    let features = status::get_susfs_features();
                    ensure!(!features.is_empty(), "SUSFS features unavailable");
                    println!("{features}");
                }
            }
            Ok(())
        }
        _ if command == "show" => bail!("unknown SUSFS query: {query}"),
        _ => {
            ensure!(
                status::get_susfs_status(),
                "SUSFS kernel interface unavailable"
            );
            match command.as_str() {
                "add_sus_path" => {
                    expect_len(args, 2)?;
                    paths::add_sus_path(&args[1])
                }
                "add_sus_path_loop" => {
                    expect_len(args, 2)?;
                    paths::add_sus_path_loop(&args[1])
                }
                "add_sus_map" => {
                    expect_len(args, 2)?;
                    paths::add_sus_map(&args[1])
                }
                "add_open_redirect" => {
                    expect_len(args, 4)?;
                    paths::add_open_redirect(
                        &args[1],
                        &args[2],
                        args[3].parse().context("invalid redirect UID scheme")?,
                    )
                }
                "add_sus_kstat" => {
                    expect_len(args, 2)?;
                    kstat::add_sus_kstat(&args[1])
                }
                "update_sus_kstat" => {
                    expect_len(args, 2)?;
                    kstat::update_sus_kstat(&args[1])
                }
                "update_sus_kstat_full_clone" => {
                    expect_len(args, 2)?;
                    kstat::update_sus_kstat_full_clone(&args[1])
                }
                "add_sus_kstat_statically" => {
                    expect_len(args, 14)?;
                    kstat::add_sus_kstat_statically(
                        &args[1],
                        args[2].parse().context("invalid inode")?,
                        args[3].parse().context("invalid device")?,
                        args[4].parse().context("invalid link count")?,
                        args[5].parse().context("invalid size")?,
                        args[6].parse().context("invalid atime seconds")?,
                        args[7].parse().context("invalid atime nanoseconds")?,
                        args[8].parse().context("invalid mtime seconds")?,
                        args[9].parse().context("invalid mtime nanoseconds")?,
                        args[10].parse().context("invalid ctime seconds")?,
                        args[11].parse().context("invalid ctime nanoseconds")?,
                        args[12].parse().context("invalid blocks")?,
                        args[13].parse().context("invalid block size")?,
                    )
                }
                "set_uname" => {
                    expect_len(args, 3)?;
                    spoof::set_uname(&args[1], &args[2])
                }
                "enable_log" => {
                    expect_len(args, 2)?;
                    spoof::enable_log(enabled(&args[1])?)
                }
                "enable_avc_log_spoofing" => {
                    expect_len(args, 2)?;
                    spoof::enable_avc_log_spoofing(enabled(&args[1])?)
                }
                "hide_sus_mnts_for_non_su_procs" => {
                    expect_len(args, 2)?;
                    spoof::hide_sus_mnts_for_non_su_procs(enabled(&args[1])?)
                }
                "set_cmdline_or_bootconfig" => {
                    expect_len(args, 2)?;
                    spoof::set_cmdline_or_bootconfig(&args[1])
                }
                _ => bail!("unknown SUSFS command: {command}"),
            }
        }
    }
}
