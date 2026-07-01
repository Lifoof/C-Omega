# C-Omega: Clinical Overall and Multi-organ Evolutionary Guided Age

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 📖 Overview

**C-Omega** (Clinical Overall and Multi-organ Evolutionary Guided Age) is a clinically actionable biological age assessment system that quantifies both **systemic** and **organ-specific** aging trajectories. Leveraging high-dimensional clinical phenotypes from a large prospective Chinese cohort (n=187,998), it provides sex‑specific models to identify not only *whether* aging is accelerated, but *where* (which organ system) the acceleration occurs.

This repository contains the core model implementation, feature engineering pipelines, and evaluation scripts used in our study.

---

## ✨ Key Features

- **Systemic biological age** with high accuracy (MAE: 2.89 yr/male, 2.20 yr/female; R²: 0.76/male, 0.85/female)
- **Nine organ‑specific models** (e.g., cardiovascular, pulmonary, immune, nutritional, digestive, hematopoietic, musculoskeletal, endocrine, renal.)
- **Sex‑specific** modeling capturing hormonal (females) and functional decline (males) patterns
- **Prognostic utility** – AgeGap correlates with current disease burden, future incidence of 73 diseases and clinical events, and mortality
- **SHAP‑based interpretability** for feature contribution analysis

---

## 🔗 Online Platform: [https://omega.nanhulab.ac.cn/](https://omega.nanhulab.ac.cn/) 

---

## 🛠️ Environment Dependencies

### Frontend

| Technology | Version |
|------------|---------|
| Node.js | 18+ (recommended: 20.18 LTS) |
| npm / Yarn | npm 9+ / Yarn 1.22+ |
| Vue | 3.5.26 |
| Vite | 6.4.1 |
| Element Plus | 2.13.1 |
| Pinia | 3.0.4 |
| Vue Router | 4.6.4 |
| Axios | 1.13.2 |
| ECharts | 5.6.0 |
| Vue I18n | 9.14.4 |
| @vueuse/core | 14.1.0 |
| @element-plus/icons-vue | 2.3.2 |
| xlsx | 0.18.5 |
| jsencrypt | 3.3.2 |
| vue-cropper | 1.1.1 |
| vuedraggable | 4.1.0 |

---

### Backend

| Technology | Version |
|------------|---------|
| Java (JDK) | 17+ |
| Maven | 3.6+ |
| Spring Boot | 4.0.3 (embedded Tomcat 11.0.18) |
| MySQL | 8.0.x (8.0.33+ or 8.4 LTS recommended; 5.7+ supported) |
| mysql-connector-j | 9.6.0 |
| Druid | 1.2.28 |
| Redis | 6.2+ or 7.x (default port: 6379) |
| Lettuce (via Spring Boot Starter) | 6.8.2.RELEASE |
| MyBatis Spring Boot Starter | 4.0.1 |
| PageHelper | 2.1.1 |
| Spring Security + JWT (jjwt 0.9.1) | 4.0.3 |
| Quartz | 2.5.2 |
| SpringDoc OpenAPI | 3.0.2 |

---
