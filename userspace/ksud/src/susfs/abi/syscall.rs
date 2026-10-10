//! Low-level syscall helper.
//!
//! Every SuSFS command is dispatched through the same `reboot(2)` syscall:
//!
//! ```text
//! syscall(SYS_reboot,
//!         KSU_INSTALL_MAGIC1,   // magic #1
//!         SUSFS_MAGIC,          // magic #2
//!         cmd_id,               // which command to run
//!         &mut payload)         // command-specific #[repr(C)] struct
//! ```
//!
//! The syscall return value and the payload error must both report success.

use libc::SYS_reboot;

use super::consts::{ERR_CMD_NOT_SUPPORTED, KSU_INSTALL_MAGIC1, SUSFS_MAGIC};
use super::types::{
    SusfsAvcLogSpoofing, SusfsCmdlineOrBootconfig, SusfsFeatures, SusfsHideSusMnts, SusfsKstat,
    SusfsLog, SusfsMap, SusfsOpenRedirect, SusfsSusPath, SusfsUname, SusfsVariant, SusfsVersion,
};

/// Dispatch a SuSFS command and return `Ok(())` when the kernel reports no
/// error.
///
/// The sentinel remains unchanged if a kernel does not handle the command.
pub fn send<T: HasErr>(cmd: u32, payload: &mut T, op_name: &str) -> anyhow::Result<()> {
    // Pass the payload as a raw `*mut c_void` so the `&mut T` reference
    // isn't moved into the syscall (the FFI signature expects a raw
    // pointer, and the kernel writes back into our buffer, but we still
    // own the `&mut T` on the Rust side).
    let result = unsafe {
        libc::syscall(
            SYS_reboot,
            KSU_INSTALL_MAGIC1,
            SUSFS_MAGIC,
            cmd,
            std::ptr::from_mut::<T>(payload).cast::<libc::c_void>(),
        )
    };

    if result == -1 {
        anyhow::bail!(
            "{op_name}: syscall failed: {}",
            std::io::Error::last_os_error()
        );
    }

    let err = payload.err();
    if err == ERR_CMD_NOT_SUPPORTED {
        anyhow::bail!("{op_name}: command not supported by the kernel");
    }
    if err == 0 {
        return Ok(());
    }
    anyhow::bail!("{op_name}: kernel returned err={err}")
}

/// Convenience wrapper: read the `err` field of a SuSFS payload struct.
///
/// Kept as a trait (instead of free-standing `field` access) so the
/// implementation lives next to the struct definitions and new payload
/// types automatically get the right implementation.
pub trait HasErr {
    fn err(&self) -> i32;
}

// `HasErr` is implemented by every concrete payload type via a blanket
// macro that reads the trailing `err` field.
macro_rules! impl_has_err {
    ($($t:ty),* $(,)?) => {
        $(impl HasErr for $t {
            #[inline]
            fn err(&self) -> i32 { self.err }
        })*
    };
}

impl_has_err!(
    SusfsVersion,
    SusfsFeatures,
    SusfsVariant,
    SusfsUname,
    SusfsLog,
    SusfsAvcLogSpoofing,
    SusfsHideSusMnts,
    SusfsOpenRedirect,
    SusfsKstat,
    SusfsMap,
    SusfsSusPath,
    SusfsCmdlineOrBootconfig,
);
