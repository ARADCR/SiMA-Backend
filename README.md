# SiMA-Backend

# SIMA — Sistema Inteligente de Monitoreo para Adultos

SIMA es una plataforma diseñada para apoyar el monitoreo y asistencia de personas adultas mayores mediante la integración de un chatbot, dispositivos IoT y notificaciones inteligentes.

El sistema permite consultar si un medicamento fue tomado, revisar eventos detectados por dispositivos como un pastillero inteligente o una pulsera IoT, y enviar alertas a familiares o cuidadores a través de WeChat.

---

## Descripción del proyecto

SIMA nace como una solución tecnológica enfocada en la salud y el acompañamiento de adultos mayores, especialmente en contextos donde es importante tener seguimiento básico de medicación, actividad y posibles riesgos.

El sistema combina:

- una interfaz web moderna,
- un backend robusto,
- procesamiento o lógica auxiliar en Python,
- dispositivos IoT basados en ESP32,
- e integración con WeChat para notificaciones.

---

## Objetivo

Brindar una herramienta centralizada que permita consultar, monitorear y alertar sobre eventos relevantes relacionados con el cuidado de adultos mayores.

---

## Funcionalidades principales

- Consulta del estado de medicamentos.
- Registro de apertura del pastillero IoT.
- Monitoreo básico de actividad o señales mediante pulsera IoT.
- Chatbot para consultas en lenguaje natural.
- Alertas a familiares o cuidadores.
- Historial de eventos y monitoreo.
- Integración con WeChat para notificaciones.
- Panel web para visualización de información.

---

## Arquitectura general

El proyecto se organiza en los siguientes componentes:

- **Frontend:** Angular
- **Backend:** Spring Boot
- **Procesamiento auxiliar / reglas:** Python
- **Dispositivo IoT:** ESP32
- **Notificaciones:** WeChat
- **Persistencia:** Base de datos relacional

### Flujo general
1. El usuario o familiar interactúa con el sistema mediante el chatbot o la interfaz web.
2. El backend procesa la solicitud y consulta la información disponible.
3. Los dispositivos IoT reportan eventos relevantes al sistema.
4. El sistema evalúa reglas o condiciones.
5. Si existe una alerta, se notifica al familiar o cuidador por WeChat.

---

## Pila tecnológica

- **Frontend:** Angular
- **Backend:** Spring Boot
- **Lenguaje auxiliar:** Python
- **Hardware IoT:** ESP32
- **Mensajería / integración:** WeChat
- **Base de datos:** MySQL

---

## Metodología de desarrollo

El proyecto se desarrollará bajo un enfoque ágil, utilizando Scrum y trabajo por sprints para organizar entregas incrementales.

---

## Estructura esperada del proyecto

La estructura del repositorio puede organizarse de la siguiente manera:

```bash
SIMA/
├── frontend/
├── backend/
├── python-services/
├── iot/
├── docs/
├── README.md
└── .gitignore
