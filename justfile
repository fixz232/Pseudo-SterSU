alias bk := build_ksud
alias bm := build_manager

build_ksuinit:
    cross build --package ksuinit --target aarch64-unknown-linux-musl --release
    cp target/aarch64-unknown-linux-musl/release/ksuinit userspace/ksud/bin/aarch64/ksuinit

build_ksud: build_ksuinit
    cross build --package ksud --target aarch64-linux-android --release

build_manager: build_ksud
    mkdir -p manager/app/src/main/jniLibs/arm64-v8a
    cp target/aarch64-linux-android/release/ksud manager/app/src/main/jniLibs/arm64-v8a/libksud.so
    cd manager && ./gradlew aDebug

clippy:
    cargo fmt
    cross clippy --target aarch64-linux-android --release
