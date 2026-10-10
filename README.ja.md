<div align="center">
  <h1>SterSU</h1>
  <p>KernelSU から派生した Android 向けカーネルベースの Root マネージャー</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <strong>日本語</strong> ·
    <a href="README.ko.md">한국어</a> ·
    <a href="README.es.md">Español</a>
  </p>
  <p><a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a></p>
</div>

SterSU は [KernelSU](https://github.com/tiann/KernelSU) から派生したオープンソースプロジェクトで、GKI と LKM の利用を想定しています。Root 権限の許可とモジュール管理を維持しながら、カーネルの保守、マネージャーの UI、オプション機能を拡張しています。一部の実装は [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) などを参考にしていますが、SterSU は KernelSU または SukiSU-Ultra の公式配布版ではありません。

## 主な機能

- **Root とモジュール**：アプリの権限を管理し、モジュールのインストール、有効化、無効化を行います。
- **GKI / LKM の保守**：KMI の適合、カーネルのインストール、イメージへのパッチを扱います。利用できる機能は端末とカーネルのビルドによって異なります。
- **Dynamic Manager**：インストール済みの互換アプリ 1 つに副マネージャー権限を付与できます。これは Root 管理の全権限に相当します。有効化する前に[安全性と使用方法](./docs/DYNAMIC_MANAGER.md)をお読みください。
- **GKI KPM**：`CONFIG_KSU=y` と `CONFIG_KPM=y` を有効にした AArch64 GKI カーネルで互換インターフェースを提供します。LKM は独立した KPatch-Next バックエンドを使用します。[KPM の由来と互換性に関する説明](./docs/SUKISU_KPM_NOTICE.md)を参照してください。
- **ABK Control**：`CONFIG_ABK_CONTROL` が有効な場合に互換ブリッジを提供し、マネージャーのパッケージ名、証明書サイズ、SHA-256 を検証します。[ABK Control の説明](./docs/ABK_CONTROL.md)を参照してください。
- **UI 拡張**：複数のマネージャー画面スタイルと、任意で利用できるストア機能を提供します。

## サイドバーの天気

天気データを使うには、設定画面の説明を読んで同意し、有効化する必要があります。データ提供元を選ぶだけでは問い合わせは始まりません。提供元ごとの取り扱いは次のとおりです。

- **Xiaomi Weather**：位置情報の権限を要求せず、端末内の天気プロバイダーからデータを読み取ります。プロバイダーが利用できない場合、ウィジェットには利用不可と表示されます。[Xiaomi Weather インターフェースの説明](./docs/XIAOMI_WEATHER_PROVIDER.md)を参照してください。
- **Open-Meteo**：ユーザーが入力した緯度・経度を外部の天気サービスに送信します。サービス側にはネットワークの IP アドレスも見えます。任意の表示名は端末内にのみ保存されます。利用条件と帰属表示は [Open-Meteo インターフェースの説明](./docs/OPEN_METEO_SIDEBAR.md)を参照してください。

## ライセンスと出典

SterSU は上流プロジェクトのライセンス区分を維持します。個別ファイルの表記を優先してください。

- `kernel/` は、個別ファイルに別の表記がない限り **GPL-2.0-only** です。
- `kernel/` 以外の KernelSU 派生コードは **GPL-3.0-or-later** です。
- サードパーティーのファイルは各自のライセンスと著作権表示を維持します。[THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) と [NOTICE](./NOTICE) を参照してください。

[Pseudo-SterSU](https://github.com/fixz232/Pseudo-SterSU) は SterSU のソースコードのバックアップ公開先であり、ライセンスを変更した版ではありません。ルートの [LICENSE](./LICENSE) には GPL バージョン 3 の本文が含まれますが、`kernel/` のライセンスは変更しません。ビルド成果物を配布する場合は、適用されるライセンスを守り、出典と表示を残し、対応する完全なソースコードを提供してください。[GPL 遵守に関する説明](./GPL-COMPLIANCE.md)も参照してください。

UI はオープンソースのデザインを参考にしています。SterSU の MIUI 風スタイルは Xiaomi の公式製品ではなく、Xiaomi のソースコードを使用していません。サイドバーのデザイン上の参考元は [Aster UI の出典説明](./docs/ASTER_UI_DESIGN_NOTICE.md)を参照してください。

## 使用前の注意

- カーネルの変更、イメージの書き込み、モジュールのインストールにより、起動不能、データ消失、端末の損傷が起こる場合があります。端末とカーネルの互換性を確認し、事前にバックアップしてください。本プロジェクトは修理、補償、アフターサービスを提供しません。
- 金融、ゲーム、企業、行政関連のアプリは Root 化された端末での利用を制限する場合があります。SterSU は検出の回避を保証せず、アカウントの異議申し立てや制限解除も支援しません。
- 自分が所有するか管理権限を得た端末でのみ、適法に使用してください。無許可の権限変更、アプリの不正解析、データ窃取、悪意ある同梱、不正行為には使用しないでください。
- 本プロジェクトに公式の有料販売やカスタマイズサービスはありません。第三者が提供する APK や改変版の出所、完全性、安全性は各自で確認してください。
- 使用前に本ページ、適用されるライセンス、各機能の文書をお読みください。リスクを受け入れられない場合は、インストールや書き込みをしないでください。

## 謝辞

- [KernelSU](https://github.com/tiann/KernelSU)：主要な上流プロジェクト。weishu とすべての貢献者に感謝します。
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) と [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu)：SuSFS の統合とパッチの出典。
- [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU)：Dynamic Manager の設計とインターフェースの参考元。
- [FolkPatch](https://github.com/LyraVoid/FolkPatch)、[Aster](https://github.com/LyraVoid/Aster)、[SKRoot](https://github.com/abcz316/SKRoot-linuxKernelRoot)：UI コードとデザインの参考元。
- [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU)：カーネルモジュールファイルの出典。
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/) と [Magisk](https://github.com/topjohnwu/Magisk)：技術的な着想元。
- [genuine](https://github.com/brevent/genuine/) と [Diamorphine](https://github.com/m0nad/Diamorphine)：署名検証と低レベル実装の参考元。
