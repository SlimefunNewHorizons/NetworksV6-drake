> ## 🐉 DrakesCraft · NetworksV6-Drake (fusión soberana)
> Este repo unifica **NetworksExpansion (Igdrassil / ytdd9527)** como base —con todas sus máquinas (auto-crafters de multibloque, line-transfer, grids, blueprints, etc.)— y le aplica encima las **mejoras drake** de NetworksV6: guard del Network Configurator, velocidad dinámica, puente de interoperabilidad con **MultiverseNets** y mirror de auditoría write-only para quantum storage. Se conservan por compatibilidad los ids `NTW_ADVANCED_*` y `NTW_QUANTUM_STORAGE` de la era V6 para **no romper ítems de los jugadores**. El plugin sigue llamándose `Networks` (data-folder intacto).

<<<<<<< HEAD
<p align="center">
  <img src="banner.svg" width="100%" alt="NETWORKSEXPANSION Animated Banner" />
</p>

# Networks Expansion — Fork de DrakesCraft Labs

Sistema integral de **almacenamiento digital, transporte cuántico de objetos, energía y fluidos para Slimefun**, inspirado en *Applied Energistics 2 / Refined Storage*. Este repositorio integra la base de Networks con más de 34.000 líneas de expansión, corregidas y adaptadas por **DrakesCraft Labs** para Paper/Purpur 1.21.11 en Java 21.

---

## 🎯 Arquitectura y Componentes

| Módulo | Autoría | Funcionalidad |
|---|---|---|
| **Base Core** | `Sefiraat` (*Networks*) | Rejillas digitales, celdas de almacenamiento cuántico, puentes y buses de importación/exportación. |
| **Expansión** | `balugaq` & `ytdd9527` | Maquinaria pesada, nodos inalámbricos de largo alcance y sintetizadores de red. |
| **Parches Drake** | `DrakesCraft-Labs` | Corrección de fugas de memoria en `SELECTED_DIRECTION_MAP`, optimización asíncrona y compatibilidad Paper 1.21.11 bajo `cl.jackstar`. |

---

## ⚡ Características Principales

- **Almacenamiento Cuántico Ilimitado**:
  - Discos y celdas de almacenamiento masivo con indexación instantánea.
- **Logística Inalámbrica & Puentes Cuánticos**:
  - Conexión de redes a través de dimensiones (Overworld, Nether, End y Mundos Espaciales de Galaxyfun).
- **Protección de Rendimiento**:
  - Límites de cálculo por tick y monitoreo integrado para evitar sobrecargas del bucle principal.

---

## 🛠️ Entorno y Compatibilidad

- **Servidor**: Paper / Purpur 1.21.11
- **Java**: 21
- **Dependencias**:
  - `Slimefun4-Drake`
=======
<div align="center">

  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/NetworksExpansion-Igdrassil/main/banner.svg" alt="NetworksExpansion-Igdrassil Banner" width="920" />

# ⚡ NetworksExpansion-Igdrassil

**SLIMEFUN4 ADDON · DRAKES EDITION**

<p>
  <a href="https://github.com/DrakesCraft-Labs/NetworksExpansion-Igdrassil"><img src="https://img.shields.io/badge/GitHub-NetworksExpansion-Igdrassil-181717?style=for-the-badge&logo=github" alt="GitHub"/></a>
  <img src="https://img.shields.io/badge/Slimefun4-Drake_Edition-22C55E?style=for-the-badge&logo=curseforge&logoColor=white" alt="Slimefun4"/>
  <img src="https://img.shields.io/badge/Paper-1.21.11-38BDF8?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
  <img src="https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
</p>

</div>

> ### 🏰 ¡Únete a la Comunidad Oficial de DrakesCraft!
> 
> * 🎮 **IP del Servidor**: `play.drakescraft.net` *(Java 1.21.11 & Bedrock)*
> * 💬 **Discord Oficial**: [discord.gg/drakescraft](https://discord.gg/rv3vtXZTk7)
> * 🌐 **Web & Guía**: [drakescraft.net](https://drakescraft.net) — 🛒 **Tienda**: [tienda.drakescraft.net](https://tienda.drakescraft.net)
> 
> *¡Juega con este addon y más de 80 expansiones optimizadas en vivo en nuestra network de supervivencia técnica!*

---

---

## 📖 Descripción Detallada

**NetworksExpansion-Igdrassil** es una expansión modular del ecosistema **DrakesCraft Labs** para servidores Minecraft **Paper / Purpur 1.21.11**.

Addon de Slimefun mantenido y optimizado por DrakesCraft Labs para Paper 1.21.11.

Todo el contenido, recetas y maquinaria se desbloquean e investigan directamente desde la **Guía de Slimefun (`/sf guide`)** sin necesidad de comandos especiales.

---

## ⚙️ Características y Sistemas Principales

* 🚀 **Rendimiento Optimizado**: Totalmente preparado para Java 21 sobre Paper 1.21.11, sin pausas de Garbage Collector ni telemetría externa.
* 🛡️ **Seguridad e Integridad**: Transacciones atómicas de almacenamiento y protección estricta de inventarios.
* 🎮 **Integración Total**: Compatible con Slimefun4-Drake, redes de logística NetworksV6, maquinaria pesada y economía global.

---

## 📋 Compatibilidad Técnica

| Parámetro | Requisito |
|---|---|
| **Servidor** | Paper / Purpur / Folia **1.21.11** |
| **Java** | **Java 21** LTS |
| **Core** | [Slimefun4-Drake](https://github.com/DrakesCraft-Labs/Slimefun4-Drake) |
| **Lado** | 100% Servidor (Server-side) |

---

## 📥 Instalación

1. Descarga el `.jar` de la última versión desde la pestaña Releases o Modrinth.
2. Colócalo en la carpeta `plugins/` del servidor junto a `Slimefun4-Drake.jar`.
3. Inicia o reinicia el servidor.

---

<div align="center">

**Desarrollado y Mantenido por [DrakesCraft Labs](https://github.com/DrakesCraft-Labs)**  
Licencia **GPL-3.0-only** / **MIT**.

</div>
>>>>>>> 2ac7eb3c (assets: actualizar banner canonico, icono PNG 512x512 y documentacion detallada)

---

## 📄 License & Upstream Attribution

This project is a sovereign fork maintained by [**JackStar6677-1**](https://github.com/JackStar6677-1) under [**DrakesCraft Labs**](https://github.com/DrakesCraft-Labs).

- **Original Project:** Created by the upstream authors and the open-source community.
- **DrakesCraft Optimizations:** Modernized for Paper/Purpur 1.21.11+, Java 21, high concurrency, asynchronous safety, and exploit/duplication prevention.
- **License:** Distributed under the original **GNU General Public License v3.0 (GPLv3)** (or original upstream license). See the [LICENSE](LICENSE) file for complete terms.
