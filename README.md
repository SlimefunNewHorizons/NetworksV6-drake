> [!WARNING]
> ### ⚠️ ARCHIVADO / CONSOLIDADO EN DRAKES-SUITES
> Este repositorio ha sido consolidado de forma definitiva en el monorepo oficial:  
> 👉 [**Drakes-Suites (Suite 1: DrakesTech)**](https://github.com/DrakesCraft-Labs/Drakes-Suites)  
> Todo el desarrollo activo, optimizaciones del Ticker Engine, compatibilidad con Paper 1.21.11 y preparación para 26.X se realiza exclusivamente allí.

<div align="center">

  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/NetworksV6-drake/main/banner.svg" alt="NetworksV6-drake Banner" width="920" />

# ⚡ NetworksV6-Drake

**Logística digital avanzada, almacenamiento cuántico, autocrafteo inteligente y redes de transporte masivo para Slimefun4.**

<p>
  <a href="https://github.com/DrakesCraft-Labs/NetworksV6-drake"><img src="https://img.shields.io/badge/GitHub-NetworksV6--Drake-181717?style=for-the-badge&logo=github" alt="GitHub"/></a>
  <img src="https://img.shields.io/badge/Slimefun4-Drake_Edition-22C55E?style=for-the-badge&logo=curseforge&logoColor=white" alt="Slimefun4"/>
  <img src="https://img.shields.io/badge/Paper-1.21.11-38BDF8?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
  <img src="https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
</p>

</div>

> ### 🏰 ¡Únete a la Comunidad Oficial de DrakesCraft!
> 
> * 🎮 **IP del Servidor**: `mc.drakescraft.cl` *(Java 1.21.11 & Bedrock)*
> * 💬 **Discord Oficial**: [discord.gg/drakescraft](https://discord.gg/rv3vtXZTk7)
> * 🌐 **Web & Guía**: [web.drakescraft.cl](https://web.drakescraft.cl) — 🛒 **Tienda**: [web.drakescraft.cl/store](https://web.drakescraft.cl/store.html)
> 
> *¡Juega con este addon y más de 80 expansiones optimizadas en vivo en nuestra network de supervivencia técnica!*

---

---

## 📖 ¿Qué es NetworksV6-Drake?

**NetworksV6-Drake** es el núcleo de logística industrial y almacenamiento digital de **Slimefun4**. Permite interconectar cofres, máquinas, reactores y generadores en una red unificada sin cables físicos desordenados, gestionando inventarios millonarios, enrutamiento condicional y fabricación automática en tiempo real.

---

## 🚀 Sistemas y Componentes de Red

### 🖥️ 1. Topología y Control de Red
* **Controlador de Red (Network Controller)**: El cerebro que indexa y sincroniza todos los nodos, cables ópticos y puentes (`Network Bridge`).
* **Monitor de Red & Terminal (Grid / Wireless Terminal)**: Interfaz gráfica unificada con buscador instantáneo para depositar, retirar y consultar millones de objetos en toda tu base.
* **Transmisor y Receptor Inalámbrico**: Enlace cuántico a larga distancia entre dimensiones y bases remotas.

### 📦 2. Almacenamiento Cuántico (Quantum Storage)
* **Celdas Cuánticas (Tiers 1 a 6)**: Almacenamiento digital de hasta 2.000.000.000 de unidades de un solo ítem por celda con protección contra overflow y duplicaciones.
* **Celdas Greedy**: Bloques de bloqueo y prioridad para alimentar líneas de producción continuas.

### 🔄 3. Transporte y Enrutamiento (Grabbers & Pushers)
* **Importadores y Exportadores (Grabber / Pusher)**: Extracción e inserción configurable por caras y filtros whitelist/blacklist.
* **Variantes de Alta Velocidad (High-Throughput)**: Transporte de hasta 64 ítems por tick de red para factorías masivas.
* **Aspiradores Filtrados (Vacuum)**: Recolección automática de drops en el suelo y limpieza de áreas de caída.

### ⚙️ 4. Automatización y Autocrafteo
* **Codificador de Recetas (Recipe Encoder)**: Graba recetas complejas de vanilla y Slimefun en planos cuánticos (`Blueprints`).
* **Autocrafter Automático**: Ensambla productos bajo demanda al instante cuando la red detecta escasez o pedidos desde la terminal.

---

## 🛡️ Capa de Fiabilidad y Estabilidad Drake

* **Reconciliación en Caliente**: Diagnóstico y autoreparación de nodos huérfanos con `/networks doctor` y `/networks reload`.
* **Topologías Seguras**: Si dos controladores quedan conectados, ambas máquinas se conservan; las redes se aíslan y el conflicto aparece en `/networks doctor` y `/networks inspect`.
* **Inspección Operativa**: Mira un controlador y ejecuta `/networks inspect` para consultar nodos, capacidad, energía, almacenamiento, E/S, crafters y conflictos sin modificar la red.
* **Guardas Anti-Duplicación**: Bloqueo de acciones ambiguas de inventario (clic central, collect-to-cursor) en interfaces de red.
* **Persistencia Atómica**: Las transacciones se confirman de forma atómica en `BlockStorage` antes de modificar los contenedores.

---

## 📋 Compatibilidad Técnica

| Parámetro | Requisito |
|---|---|
| **Servidor** | Paper / Purpur / Folia **1.21.11** |
| **Java** | **Java 21** LTS |
| **Core Requerido** | [Slimefun4-Drake](https://github.com/DrakesCraft-Labs/Slimefun4-Drake) |
| **Arquitectura** | 100% Server-side |

---

## 📥 Instalación

1. Descarga la última versión de `NetworksV6-drake.jar` desde la pestaña [Versions](https://modrinth.com/mod/networksv6-drake/versions).
2. Colócalo en `plugins/` junto a `Slimefun4-Drake.jar`.
3. Inicia o reinicia el servidor.

---

<div align="center">

**Desarrollado y Mantenido por [DrakesCraft Labs](https://github.com/DrakesCraft-Labs)**  
*Basado en el diseño original de Sefiraat y mmmjjkx.*  
Licencia **GPL-3.0-only**.

</div>
