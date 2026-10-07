use std::path::Path;

use serde_json::json;

use crate::{defs, ksucalls, module};

pub const KPATCH_NEXT_MODULE_ID: &str = "KPatch-Next";

// Only report an independently installed module; ksud no longer bundles one.
pub fn print_status() {
    if !ksucalls::is_lkm_mode() || ksucalls::is_late_load() {
        println!(
            "{}",
            json!({
                "moduleId": KPATCH_NEXT_MODULE_ID,
                "installed": false,
                "enabled": false,
                "webui": false,
                "builtinAvailable": false,
                "backend": "unsupported",
            })
        );
        return;
    }
    let module_dir = Path::new(defs::MODULE_DIR).join(KPATCH_NEXT_MODULE_ID);
    let update_dir = Path::new(defs::MODULE_UPDATE_DIR).join(KPATCH_NEXT_MODULE_ID);
    let module_prop = module::read_module_prop(&module_dir).unwrap_or_default();
    let pending_remove = module_dir.join(defs::REMOVE_FILE_NAME).exists()
        || update_dir.join(defs::REMOVE_FILE_NAME).exists();
    let enabled = is_enabled() && !pending_remove;
    println!(
        "{}",
        json!({
            "moduleId": KPATCH_NEXT_MODULE_ID,
            "moduleName": module_prop.get("name").map_or("KPatch-Next", String::as_str),
            "modulePath": module_dir.display().to_string(),
            "version": module_prop.get("version").map_or("", String::as_str),
            "versionCode": module_prop.get("versionCode").map_or("", String::as_str),
            "installed": module_dir.join("module.prop").is_file(),
            "enabled": enabled,
            "pendingUpdate": update_dir.join("module.prop").is_file()
                && !update_dir.join(defs::DISABLE_FILE_NAME).exists()
                && !update_dir.join(defs::REMOVE_FILE_NAME).exists(),
            "pendingRemove": pending_remove,
            "webui": module_dir.join(defs::MODULE_WEB_DIR).join("index.html").is_file(),
            "unresolved": module_dir.join("unresolved").exists(),
            "dataDir": Path::new("/data/adb/kp-next").exists(),
            "builtinAvailable": false,
            "conflict": serde_json::Value::Null,
        })
    );
}

// An independently installed module may still provide LKM's KPM backend.
pub fn is_enabled() -> bool {
    if !ksucalls::is_lkm_mode() || ksucalls::is_late_load() {
        return false;
    }
    let module_dir = Path::new(defs::MODULE_DIR).join(KPATCH_NEXT_MODULE_ID);
    let update_dir = Path::new(defs::MODULE_UPDATE_DIR).join(KPATCH_NEXT_MODULE_ID);
    is_module_enabled(&module_dir)
        && !update_dir.join(defs::DISABLE_FILE_NAME).exists()
        && !update_dir.join(defs::REMOVE_FILE_NAME).exists()
}

fn is_module_enabled(dir: &Path) -> bool {
    dir.join("module.prop").is_file()
        && !dir.join(defs::DISABLE_FILE_NAME).exists()
        && !dir.join(defs::REMOVE_FILE_NAME).exists()
}

#[cfg(test)]
mod tests {
    use super::is_module_enabled;
    use std::fs;
    use tempfile::tempdir;

    #[test]
    fn disabled_module_does_not_claim_an_enabled_runtime() {
        let root = tempdir().unwrap();
        assert!(!is_module_enabled(root.path()));

        fs::write(root.path().join("module.prop"), "id=KPatch-Next\n").unwrap();
        assert!(is_module_enabled(root.path()));

        fs::write(root.path().join("disable"), "").unwrap();
        assert!(!is_module_enabled(root.path()));
    }
}
