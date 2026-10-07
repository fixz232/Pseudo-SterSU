pub const fn is_compatible(kernel_version: u32, userspace_version: u32) -> bool {
    kernel_version == userspace_version || (kernel_version == 4 && userspace_version == 5)
}

pub const fn supports_services_report(kernel_version: u32) -> bool {
    kernel_version >= 5
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn only_the_additive_services_upgrade_is_compatible() {
        assert!(is_compatible(4, 5));
        assert!(is_compatible(5, 5));
        assert!(!is_compatible(3, 5));
        assert!(!is_compatible(6, 5));
        assert!(!is_compatible(5, 4));
        assert!(!is_compatible(4, 6));
    }

    #[test]
    fn legacy_kernels_do_not_receive_the_new_event() {
        assert!(!supports_services_report(0));
        assert!(!supports_services_report(4));
        assert!(supports_services_report(5));
        assert!(supports_services_report(6));
    }
}
