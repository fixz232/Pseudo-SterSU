<div align="center">
  <h1>SterSU</h1>
  <p>Gestor root de Android a nivel del kernel derivado de KernelSU</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <a href="README.ko.md">한국어</a> ·
    <strong>Español</strong>
  </p>
  <p><a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a></p>
</div>

SterSU es un proyecto de código abierto derivado de [KernelSU](https://github.com/tiann/KernelSU) para entornos GKI y LKM. Conserva la gestión de permisos root y módulos, y amplía el mantenimiento del kernel, la interfaz del gestor y las funciones opcionales. Algunas implementaciones toman como referencia [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) y otros proyectos; SterSU no es una versión oficial de KernelSU ni de SukiSU-Ultra.

## Funciones

- **Root y módulos**: gestiona los permisos de las aplicaciones y permite instalar, activar o desactivar módulos.
- **Mantenimiento GKI / LKM**: se centra en la compatibilidad KMI, la instalación del kernel y el parcheo de imágenes. Las funciones disponibles dependen del dispositivo y de la compilación del kernel.
- **Gestor dinámico**: permite que una aplicación compatible ya instalada obtenga autoridad como gestor secundario. Esto equivale a control total de la gestión root; lee la [guía de seguridad y uso](./docs/DYNAMIC_MANAGER.md) antes de activarlo.
- **GKI KPM**: ofrece una interfaz compatible en kernels GKI AArch64 compilados con `CONFIG_KSU=y` y `CONFIG_KPM=y`. LKM usa un motor KPatch-Next independiente. Consulta el [aviso de origen y compatibilidad de KPM](./docs/SUKISU_KPM_NOTICE.md).
- **ABK Control**: ofrece un puente de compatibilidad cuando `CONFIG_ABK_CONTROL` está habilitado y verifica el nombre del paquete, el tamaño del certificado y su SHA-256. Consulta la [guía de ABK Control](./docs/ABK_CONTROL.md).
- **Extensiones de interfaz**: ofrece varios estilos para el gestor y funciones opcionales de la tienda.

## Tiempo en la barra lateral

Para usar datos meteorológicos, lee y acepta el aviso en Ajustes. Elegir una fuente no inicia de inmediato ninguna consulta. Cada fuente trata los datos de forma distinta:

- **Tiempo de Xiaomi**: lee el proveedor meteorológico local del dispositivo sin solicitar permiso de ubicación. Si el proveedor no está disponible, el widget muestra un estado de indisponibilidad. Consulta el [aviso de la interfaz de Tiempo de Xiaomi](./docs/XIAOMI_WEATHER_PROVIDER.md).
- **Open-Meteo**: envía las coordenadas introducidas por el usuario a un servicio meteorológico externo, que también puede ver la dirección IP de la conexión. El nombre para mostrar opcional permanece en el dispositivo. Consulta el [aviso de la interfaz Open-Meteo](./docs/OPEN_METEO_SIDEBAR.md) para conocer las condiciones y la atribución.

## Licencias y procedencia

SterSU mantiene los límites de licencia de los proyectos originales. Prevalecen los avisos de cada archivo:

- El directorio `kernel/` está bajo **GPL-2.0-only**, salvo que un archivo indique lo contrario.
- El código derivado de KernelSU fuera de `kernel/` está bajo **GPL-3.0-or-later**.
- Los archivos de terceros mantienen sus respectivas licencias y avisos de derechos de autor; consulta [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) y [NOTICE](./NOTICE).

[Pseudo-SterSU](https://github.com/fixz232/Pseudo-SterSU) es un repositorio de respaldo del código fuente de SterSU, no una edición con otra licencia. El archivo [LICENSE](./LICENSE) de la raíz contiene el texto de la GPL versión 3 y no cambia la licencia de `kernel/`. Al distribuir compilaciones, respeta las licencias aplicables, conserva los avisos y la atribución, y proporciona el código fuente correspondiente completo. Consulta las [notas de cumplimiento de la GPL](./GPL-COMPLIANCE.md).

La interfaz toma como referencia diseños de código abierto. El estilo MIUI de SterSU no es un producto oficial de Xiaomi ni utiliza código fuente de Xiaomi. Consulta la [atribución de la interfaz Aster](./docs/ASTER_UI_DESIGN_NOTICE.md) para conocer las referencias de diseño de la barra lateral.

## Antes de usar

- Cambiar el kernel, flashear una imagen o instalar un módulo puede impedir el arranque, causar pérdida de datos o dañar el dispositivo. Comprueba la compatibilidad y haz una copia de seguridad antes de continuar. El proyecto no ofrece reparación, indemnización ni servicio posventa.
- Las aplicaciones financieras, los juegos y las aplicaciones empresariales o gubernamentales pueden restringir los dispositivos rooteados. SterSU no garantiza que se eviten sus controles ni ayuda con reclamaciones de cuentas o con la eliminación de restricciones.
- Úsalo legalmente solo en dispositivos propios o que estés autorizado a administrar. No lo utilices para cambiar privilegios sin permiso, vulnerar aplicaciones, robar datos, incluir software malicioso o hacer trampas.
- El proyecto no tiene venta oficial de pago ni servicio de personalización. Comprueba por tu cuenta el origen, la integridad y la seguridad de los APK de terceros o modificados.
- Lee esta página, las licencias aplicables y la documentación de las funciones antes de usarlo. No instales ni flashees nada si no aceptas estos riesgos.

## Agradecimientos

- [KernelSU](https://github.com/tiann/KernelSU): proyecto principal del que deriva SterSU; gracias a weishu y a todos sus colaboradores.
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) y [susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu): integración de SuSFS y origen de los parches.
- [ReSukiSU](https://github.com/ReSukiSU/ReSukiSU): referencias para el diseño y las interfaces del gestor dinámico.
- [FolkPatch](https://github.com/LyraVoid/FolkPatch), [Aster](https://github.com/LyraVoid/Aster) y [SKRoot](https://github.com/abcz316/SKRoot-linuxKernelRoot): referencias de código y diseño de la interfaz.
- [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU): origen de los archivos de módulos del kernel.
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/) y [Magisk](https://github.com/topjohnwu/Magisk): inspiración técnica.
- [genuine](https://github.com/brevent/genuine/) y [Diamorphine](https://github.com/m0nad/Diamorphine): referencias para verificación de firmas e implementaciones de bajo nivel.
