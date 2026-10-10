<div align="center">
  <h1>SterSU</h1>
  <p>KernelSU에서 파생된 Android 커널 기반 루트 관리자</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <strong>한국어</strong> ·
    <a href="README.es.md">Español</a>
  </p>
  <p><a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a></p>
</div>

SterSU는 [KernelSU](https://github.com/tiann/KernelSU)에서 파생된 오픈 소스 프로젝트로, GKI 및 LKM 환경을 대상으로 합니다. 루트 권한 승인과 모듈 관리 기능을 유지하면서 커널 유지 관리, 관리자 UI 및 선택 기능을 확장합니다. 일부 구현은 [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) 등에서 참고했으며, SterSU는 KernelSU 또는 SukiSU-Ultra의 공식 배포판이 아닙니다.

## 주요 기능

- **루트 및 모듈**: 앱 권한을 관리하고 모듈을 설치, 활성화 또는 비활성화합니다.
- **GKI / LKM 유지 관리**: KMI 호환성 확인, 커널 설치 및 이미지 패치를 다룹니다. 사용 가능한 기능은 기기와 커널 빌드에 따라 달라집니다.
- **동적 관리자**: 설치된 호환 앱 하나에 보조 관리자 권한을 부여할 수 있습니다. 이는 전체 루트 관리 권한에 해당하므로, 활성화 전에 [보안 및 사용 안내](./docs/DYNAMIC_MANAGER.md)를 읽으세요.
- **GKI KPM**: `CONFIG_KSU=y`와 `CONFIG_KPM=y`로 빌드된 AArch64 GKI 커널에서 호환 인터페이스를 제공합니다. LKM은 별도의 KPatch-Next 백엔드를 사용합니다. [KPM 출처 및 호환성 안내](./docs/SUKISU_KPM_NOTICE.md)를 참고하세요.
- **ABK Control**: `CONFIG_ABK_CONTROL`이 활성화된 경우 호환 브리지를 제공하며 관리자 패키지 이름, 인증서 크기 및 SHA-256을 검증합니다. [ABK Control 안내](./docs/ABK_CONTROL.md)를 참고하세요.
- **UI 확장**: 여러 관리자 화면 스타일과 선택적으로 사용할 수 있는 스토어 기능을 제공합니다.

## 사이드바 날씨

날씨 데이터를 사용하려면 설정에서 고지 내용을 읽고 동의한 뒤 활성화해야 합니다. 제공자를 선택하는 것만으로 조회가 시작되지는 않습니다. 제공자별 데이터 처리 방식은 다음과 같습니다.

- **Xiaomi Weather**: 위치 권한을 요청하지 않고 기기의 로컬 날씨 제공자에서 데이터를 읽습니다. 제공자를 사용할 수 없으면 위젯에 사용 불가 상태가 표시됩니다. [Xiaomi Weather 인터페이스 안내](./docs/XIAOMI_WEATHER_PROVIDER.md)를 참고하세요.
- **Open-Meteo**: 사용자가 입력한 좌표를 외부 날씨 서비스에 전송하며, 서비스는 네트워크 IP 주소도 볼 수 있습니다. 선택적으로 입력하는 표시 이름은 기기에만 저장됩니다. 이용 조건과 출처 표시는 [Open-Meteo 인터페이스 안내](./docs/OPEN_METEO_SIDEBAR.md)를 참고하세요.

## 라이선스 및 출처

SterSU는 업스트림의 라이선스 경계를 유지합니다. 각 파일의 개별 고지가 우선합니다.

- `kernel/`은 개별 파일에 달리 명시되지 않은 한 **GPL-2.0-only**입니다.
- `kernel/` 외부의 KernelSU 파생 코드는 **GPL-3.0-or-later**입니다.
- 타사 파일은 각각의 라이선스와 저작권 고지를 유지합니다. [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) 및 [NOTICE](./NOTICE)를 참고하세요.

[Pseudo-SterSU](https://github.com/fixz232/Pseudo-SterSU)는 SterSU 소스 코드의 백업 공개 저장소이며, 라이선스를 변경한 판본이 아닙니다. 루트의 [LICENSE](./LICENSE)에는 GPL 버전 3 본문이 포함되어 있지만 `kernel/`의 라이선스를 변경하지 않습니다. 빌드 결과물을 배포할 때는 적용되는 라이선스를 준수하고 출처와 고지를 유지하며, 해당하는 전체 소스 코드를 제공해야 합니다. [GPL 준수 안내](./GPL-COMPLIANCE.md)를 참고하세요.

UI는 오픈 소스 디자인을 참고했습니다. SterSU의 MIUI 스타일은 Xiaomi 공식 제품이 아니며 Xiaomi 소스 코드를 사용하지 않습니다. 사이드바 디자인의 참고 출처는 [Aster UI 출처 안내](./docs/ASTER_UI_DESIGN_NOTICE.md)를 참고하세요.

## 사용 전 안내

- 커널 변경, 이미지 플래싱 또는 모듈 설치로 부팅 실패, 데이터 손실 또는 기기 손상이 발생할 수 있습니다. 기기와 커널의 호환성을 확인하고 미리 백업하세요. 프로젝트는 수리, 보상 또는 사후 서비스를 제공하지 않습니다.
- 금융, 게임, 기업 및 공공기관 앱은 루팅된 기기의 사용을 제한할 수 있습니다. SterSU는 탐지 우회를 보장하지 않으며 계정 이의 신청이나 제한 해제를 지원하지 않습니다.
- 소유하거나 관리 권한을 받은 기기에서만 합법적으로 사용하세요. 허가 없는 권한 변경, 앱 크래킹, 데이터 절도, 악성 프로그램 묶음 배포 또는 부정행위에 사용하지 마세요.
- 프로젝트에는 공식 유료 판매나 맞춤 제작 서비스가 없습니다. 타사가 제공하는 APK나 수정판의 출처, 무결성 및 안전성은 직접 확인해야 합니다.
- 사용 전에 이 문서, 적용되는 라이선스 및 기능 문서를 읽으세요. 위험을 받아들일 수 없다면 설치하거나 플래싱하지 마세요.

## 감사의 말

- [KernelSU](https://github.com/tiann/KernelSU): 주요 업스트림 프로젝트입니다. weishu와 모든 기여자에게 감사드립니다.
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) 및 [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu): SuSFS 통합 및 패치 출처입니다.
- [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU): 동적 관리자 설계와 인터페이스의 참고 자료입니다.
- [FolkPatch](https://github.com/LyraVoid/FolkPatch), [Aster](https://github.com/LyraVoid/Aster) 및 [SKRoot](https://github.com/abcz316/SKRoot-linuxKernelRoot): UI 코드와 디자인의 참고 자료입니다.
- [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU): 커널 모듈 파일의 출처입니다.
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/) 및 [Magisk](https://github.com/topjohnwu/Magisk): 기술적 아이디어의 참고 자료입니다.
- [genuine](https://github.com/brevent/genuine/) 및 [Diamorphine](https://github.com/m0nad/Diamorphine): 서명 검증과 저수준 구현의 참고 자료입니다.
