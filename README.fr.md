<div align="center">
  <h1>SterSU</h1>
  <p>Gestionnaire root Android au niveau du noyau, dérivé de KernelSU</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <strong>Français</strong> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <a href="README.ko.md">한국어</a> ·
    <a href="README.es.md">Español</a>
  </p>
  <p><a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a></p>
</div>

SterSU est un projet open source dérivé de [KernelSU](https://github.com/tiann/KernelSU), destiné aux environnements GKI et LKM. Il conserve la gestion des autorisations root et des modules, tout en étendant la maintenance du noyau, l'interface du gestionnaire et les fonctions facultatives. Certaines implémentations s'inspirent de [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) et d'autres projets ; SterSU n'est pas une version officielle de KernelSU ou de SukiSU-Ultra.

## Fonctionnalités

- **Root et modules** : gérer les autorisations des applications et installer, activer ou désactiver des modules.
- **Maintenance GKI / LKM** : correspondance KMI, installation du noyau et correctifs d'images. Les fonctions disponibles dépendent de l'appareil et de la compilation du noyau.
- **Gestionnaire dynamique** : permettre à une application compatible déjà installée d'obtenir les droits de gestionnaire secondaire. Ces droits donnent un contrôle complet du root ; lisez le [guide de sécurité et d'utilisation](./docs/DYNAMIC_MANAGER.md) avant l'activation.
- **GKI KPM** : fournir une interface compatible sur les noyaux GKI AArch64 compilés avec `CONFIG_KSU=y` et `CONFIG_KPM=y`. LKM utilise un moteur KPatch-Next distinct. Voir la [déclaration sur l'origine et la compatibilité de KPM](./docs/SUKISU_KPM_NOTICE.md).
- **ABK Control** : fournir une passerelle de compatibilité lorsque `CONFIG_ABK_CONTROL` est activé, avec vérification du nom de paquet, de la taille du certificat et de son empreinte SHA-256. Voir la [documentation ABK Control](./docs/ABK_CONTROL.md).
- **Extensions d'interface** : proposer plusieurs styles d'interface du gestionnaire et des fonctions de boutique facultatives.

## Météo de la barre latérale

Avant d'utiliser les données météo, lisez et acceptez la déclaration dans les paramètres. Sélectionner une source ne déclenche pas immédiatement de requête. Les sources traitent les données différemment :

- **Météo Xiaomi** : lit les données du fournisseur météo local de l'appareil sans demander l'autorisation de localisation. Si ce fournisseur est absent, le widget indique que la météo est indisponible. Voir la [déclaration sur l'interface Météo Xiaomi](./docs/XIAOMI_WEATHER_PROVIDER.md).
- **Open-Meteo** : envoie les coordonnées saisies par l'utilisateur à un service météo tiers, qui peut également voir l'adresse IP du réseau. Le nom d'affichage facultatif reste sur l'appareil. Voir la [déclaration sur l'interface Open-Meteo](./docs/OPEN_METEO_SIDEBAR.md) pour les conditions d'utilisation et l'attribution.

## Licences et provenance

SterSU conserve les limites des licences en amont. Les mentions propres à chaque fichier font foi :

- Le répertoire `kernel/` est sous licence **GPL-2.0-only**, sauf mention contraire dans un fichier.
- Le code dérivé de KernelSU hors de `kernel/` est sous licence **GPL-3.0-or-later**.
- Les fichiers tiers conservent leurs licences et mentions de droit d'auteur ; voir [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) et [NOTICE](./NOTICE).

[Pseudo-SterSU](https://github.com/fixz232/Pseudo-SterSU) est un dépôt de secours pour le code source de SterSU, sans changement de licence. Le fichier [LICENSE](./LICENSE) à la racine contient le texte de la GPL version 3 et ne modifie pas la licence de `kernel/`. Toute distribution de versions compilées doit respecter les licences applicables, conserver les mentions d'origine et fournir le code source correspondant complet. Voir les [notes de conformité GPL](./GPL-COMPLIANCE.md).

L'interface s'inspire de projets open source. Le style MIUI de SterSU n'est pas un produit officiel Xiaomi et n'utilise pas le code source de Xiaomi. Voir la [déclaration relative à l'interface Aster](./docs/ASTER_UI_DESIGN_NOTICE.md) pour les références de conception de la barre latérale.

## Avant utilisation

- Modifier le noyau, flasher une image ou installer un module peut empêcher le démarrage, entraîner une perte de données ou endommager l'appareil. Vérifiez la compatibilité et sauvegardez vos données au préalable. Le projet ne fournit ni réparation, ni indemnisation, ni service après-vente.
- Les applications financières, les jeux et les applications professionnelles ou administratives peuvent restreindre l'usage d'appareils rootés. SterSU ne garantit pas le contournement de leurs contrôles et n'aide pas aux recours liés aux comptes ou à la levée des restrictions.
- N'utilisez le projet légalement que sur des appareils qui vous appartiennent ou que vous êtes autorisé à administrer. Ne l'utilisez pas pour modifier des privilèges sans autorisation, craquer des applications, voler des données, intégrer des logiciels malveillants ou tricher.
- Le projet ne propose ni vente officielle payante ni service de personnalisation. Vérifiez vous-même la provenance, l'intégrité et la sécurité des APK tiers ou modifiés.
- Lisez cette page, les licences applicables et la documentation des fonctions avant utilisation. N'installez ou ne flashez rien si vous n'acceptez pas ces risques.

## Remerciements

- [KernelSU](https://github.com/tiann/KernelSU) : projet en amont principal ; merci à weishu et à tous les contributeurs.
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) et [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu) : intégration de SuSFS et sources des correctifs.
- [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU) : références pour la conception et les interfaces du gestionnaire dynamique.
- [FolkPatch](https://github.com/LyraVoid/FolkPatch), [Aster](https://github.com/LyraVoid/Aster) et [SKRoot](https://github.com/abcz316/SKRoot-linuxKernelRoot) : références pour le code et la conception de l'interface.
- [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU) : source des fichiers de modules du noyau.
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/) et [Magisk](https://github.com/topjohnwu/Magisk) : inspirations techniques.
- [genuine](https://github.com/brevent/genuine/) et [Diamorphine](https://github.com/m0nad/Diamorphine) : références pour la vérification des signatures et les implémentations de bas niveau.
