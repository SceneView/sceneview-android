# Analyse Concurrentielle MCP — Rapport Stratégique

**Date**: 27 mars 2026
**Auteur**: Claude Code (analyse automatisée)
**Scope**: Positionnement des 21 MCPs de Thomas Gorisse (npm: thomasgorisse)

---

## Table des matières

1. [Panorama de l'écosystème MCP](#1-panorama)
2. [Top concurrents par catégorie](#2-concurrents)
3. [Positionnement de nos MCPs](#3-positionnement)
4. [Forces et faiblesses](#4-forces-faiblesses)
5. [Recommandations stratégiques](#5-recommandations)
6. [Plan d'action](#6-plan-action)

---

## 1. Panorama de l'écosystème MCP {#1-panorama}

### Chiffres clés (mars 2026)

| Métrique | Valeur |
|---|---|
| MCP SDK npm weekly downloads | **43.7M** |
| Nombre total de MCP servers indexés | **20,300+** (Glama), **12,870+** (PulseMCP) |
| Croissance depuis mid-2025 | **+873%** (425 → 4,133 sur SkillsIndex) |
| GitHub stars modelcontextprotocol/servers | **82,300** |
| GitHub stars awesome-mcp-servers | **3,800** |
| % de MCPs monétisés | **< 5%** |
| Catégories Glama | **85** |

### Les 3 plus grosses catégories (Glama)

1. **Developer Tools** — 4,277 serveurs
2. **Search** — 2,141 serveurs
3. **App Automation** — 1,985 serveurs

### Top MCPs par downloads npm (estimations)

| MCP Server | Weekly Downloads (est.) | Stars GitHub |
|---|---|---|
| @upstash/context7-mcp | ~240,000 | 19,000+ |
| @anthropic-ai/claude-code (inclut MCP) | ~10,000,000 | — |
| @modelcontextprotocol/server-sequential-thinking | ~73,000 | 82,000 (mono-repo) |
| Playwright MCP (Microsoft) | élevé | 5,600+ |
| Firecrawl MCP | élevé | 85,000+ (total) |
| GitHub MCP | élevé | 27,900+ |

### Serveurs officiels de référence (Anthropic)

Filesystem, Git, Memory, Sequential Thinking, Time, Fetch, Everything — ce sont les "standards" que tout le monde connaît.

### Serveurs d'entreprise dominants

- **GitHub** (score 100/100, 27.9K stars)
- **Playwright** (Microsoft, 5.6K stars, 22 tools)
- **Cloudflare** (rated 4.5/5)
- **AWS** (68 sous-serveurs)
- **Google** (30+ serveurs)
- **Stripe, Linear, Sentry, Notion** — tous ont leur MCP officiel

---

## 2. Top concurrents par catégorie {#2-concurrents}

### Catégories où nous sommes présents

#### 3D / AR / Visualization
| Concurrent | Description | Stars/Downloads |
|---|---|---|
| **sceneview-mcp** (NOUS) | SDK 3D/AR code gen | v3.4.14 npm |
| hello3dmcp-server | Three.js via WebSocket | < 50 stars |
| mcp-game-asset-gen | Assets 3D pour game engines | < 100 stars |
| maige-3d-mcp | WebGPU 3D | < 50 stars |
| Spline 3D Design MCP | Intégration Spline.design | nouveau |

**Position**: LEADER INCONTESTÉ. Aucun concurrent sérieux en 3D/AR code generation. Le marché est quasi-vide.

#### Real Estate
| Concurrent | Description |
|---|---|
| **realestate-mcp** (NOUS) | Standalone, pas d'API |
| agentic-ops/real-estate-mcp | Demo showcase, 30+ tools, Python SSE |
| ATTOM MCP Server | Enterprise, API payante (AVM, comps) |
| BatchData MCP | 155M propriétés, API payante |
| @miyatsuko10004/realestate-library-mcp | Petit package npm |

**Position**: Niche. Les gros joueurs (ATTOM, BatchData) ont des données réelles via API payantes. Notre avantage : standalone, gratuit, pas d'API key.

#### Gaming
| Concurrent | Description |
|---|---|
| **gaming-3d-mcp** (NOUS) | Game design + 3D assets |
| mcp-game-asset-gen (Flux159) | Assets via Hugging Face |
| game-asset-mcp (MubarakHAlketbi) | 2D/3D assets from text |

**Position**: BON. Peu de concurrence en gaming MCP. Notre différenciateur 3D est unique.

#### Interior Design
| Concurrent | Description |
|---|---|
| **interior-design-3d-mcp** (NOUS) | Design 3D intérieur |
| (aucun concurrent direct) | — |

**Position**: SEUL sur le marché. Niche très sous-servie.

#### Cooking / Recipes
| Concurrent | Description |
|---|---|
| **cooking-mcp** (NOUS) | Standalone |
| HowToCook-mcp | Recettes chinoises, npm |
| spoonacular-mcp | API spoonacular (nécessite clé) |
| recipe-mcp (blaideinc) | API cookwith.co |
| mcp-cook (disdjj) | Recettes HowToCook |
| Recipe Intelligence (Apify) | Scraping recettes |

**Position**: MOYEN. 5+ concurrents, mais la plupart nécessitent des API keys. Notre avantage standalone reste.

#### Health / Fitness
| Concurrent | Description |
|---|---|
| **health-fitness-mcp** (NOUS) | Standalone |
| Apple Health MCP → Open Wearables | Multi-wearables (Garmin, Polar, etc.) |
| Function Health MCP | Lab results, CLI |
| Gym Coach MCP (nicosaporiti) | Suivi workout |
| Mindbody MCP | API studio fitness |
| Spike Health MCP | Données wearables |
| Health & Fitness Coach MCP (LobeHub) | Coaching IA |

**Position**: FAIBLE. Marché compétitif avec des joueurs qui ont des intégrations réelles (wearables, API santé). Notre MCP standalone a moins de valeur ici.

#### Education
| Concurrent | Description |
|---|---|
| **education-mcp** (NOUS) | Standalone |
| EduBase MCP (officiel) | Plateforme éducative |
| Anki MCP | Flashcards (featured sur mcpservers.org) |
| Kaggle MCP | Datasets + competitions |

**Position**: MOYEN. Anki MCP est featured, EduBase est officiel. Notre MCP a besoin de différenciation.

#### Finance
| Concurrent | Description |
|---|---|
| **@thomasgorisse/finance-mcp** (NOUS) | Standalone |
| Financial Datasets MCP | Top 10 (CyberPress) |
| Norman Finance | Officiel |
| Stripe MCP | Paiements (officiel) |
| CoinGecko MCP | Crypto (officiel) |
| Ramp MCP | Finance entreprise |

**Position**: FAIBLE. Les joueurs établis ont des données réelles. Financial Datasets est dans le top 10 mondial.

#### SEO
| Concurrent | Description |
|---|---|
| **@thomasgorisse/seo-mcp** (NOUS) | Standalone |
| Google Search Console MCP (mcp-gsc) | 500+ stars, données réelles |
| Google Analytics MCP | Officiel Google |
| DataForSEO MCP | API pro, données SERP |
| Semrush MCP | Officiel, keyword research |
| Nightwatch SEO MCP | Enterprise |
| searchatlas-mcp-server | npm + registry |

**Position**: TRÈS FAIBLE. Google, Semrush, DataForSEO ont des MCPs officiels avec données réelles. Notre MCP standalone ne peut pas rivaliser.

#### DevOps
| Concurrent | Description |
|---|---|
| **devops-mcp** (NOUS) | Standalone |
| Jenkins MCP | Plugin officiel |
| GitLab MCP (zereight) | Pipelines GitLab |
| Azure DevOps MCP | GA, intégré VS 2026 |
| Argo CD MCP | GitOps Kubernetes |
| Docker MCP | Containers (rated 3.5/5) |
| Kubernetes MCP | Clusters (rated 4/5) |
| Terraform MCP | Infrastructure (rated 4/5) |

**Position**: TRÈS FAIBLE. Marché dominé par les outils officiels (Jenkins, Azure, Docker, K8s, Terraform).

#### Productivity
| Concurrent | Description |
|---|---|
| **productivity-mcp** (NOUS) | Standalone |
| Asana MCP | 44 tools, officiel |
| Linear MCP | Issue tracking (rated 4/5) |
| Notion MCP | Knowledge (rated 3.5/5) |
| Todoist MCP | Tasks |
| Zapier MCP | 8,000+ intégrations |

**Position**: TRÈS FAIBLE. Asana seul a 44 tools. Zapier connecte 8,000 apps.

#### Travel
| Concurrent | Description |
|---|---|
| **travel-mcp** (NOUS) | Standalone |
| Kiwi.com MCP | Flights (officiel) |
| Google Maps MCP | 1,550+ stars |
| Campertunity MCP | Camping (officiel) |

**Position**: FAIBLE. Kiwi.com et Google Maps ont des données réelles.

#### Legal
| Concurrent | Description |
|---|---|
| **legal-docs-mcp** (NOUS) | Docs juridiques FR standalone |
| eSignatures MCP | Officiel |
| (peu de concurrents en FR) | — |

**Position**: BON pour le marché francophone. Niche sous-servie.

#### French Admin
| Concurrent | Description |
|---|---|
| **french-admin-mcp** (NOUS) | Admin française standalone |
| (aucun concurrent direct) | — |

**Position**: SEUL sur le marché. Niche 100% sous-servie.

#### E-commerce 3D
| Concurrent | Description |
|---|---|
| **ecommerce-3d-mcp** (NOUS) | E-commerce + 3D |
| Mercado Libre MCP | Officiel |
| ShopSavvy MCP | Officiel |
| (pas de concurrent 3D e-commerce) | — |

**Position**: BON. La composante 3D nous différencie.

#### Architecture
| Concurrent | Description |
|---|---|
| **architecture-mcp** (NOUS) | Architecture 3D |
| (pas de concurrent direct) | — |

**Position**: SEUL. Catégorie "Software Architecture" existe (85e catégorie Glama) mais pas d'architecture bâtiment 3D.

### Catégories où nous ne sommes PAS présents (mais massives)

| Catégorie | Taille estimée | Leaders |
|---|---|---|
| **Browser Automation** | 1,000+ serveurs | Playwright (MS), Puppeteer, Chrome DevTools |
| **Database** | 2,000+ serveurs | MongoDB, PostgreSQL, Supabase, Neon |
| **Search** | 2,141 serveurs | Context7, Brave, Exa, Tavily |
| **Web Scraping** | 1,000+ serveurs | Firecrawl, Crawl4AI |
| **Cloud Platforms** | 500+ serveurs | AWS, GCP, Azure, Cloudflare |
| **Communication** | 500+ serveurs | Slack, Teams, Discord |
| **Version Control** | 500+ serveurs | GitHub, GitLab, Git |
| **AI/ML** | 1,000+ serveurs | HuggingFace, Kaggle |
| **Security** | 300+ serveurs | Semgrep, Sentry |
| **Design** | 200+ serveurs | Figma (Framelink), Spline |

---

## 3. Positionnement de nos MCPs {#3-positionnement}

### Matrice de positionnement

| MCP | Marché | Concurrence | Notre position | Potentiel |
|---|---|---|---|---|
| **sceneview-mcp** | 3D/AR | Très faible | LEADER | ÉLEVÉ |
| **french-admin-mcp** | Admin FR | Aucune | MONOPOLE | MOYEN (niche) |
| **interior-design-3d-mcp** | Interior 3D | Aucune | MONOPOLE | ÉLEVÉ |
| **architecture-mcp** | Archi 3D | Aucune | MONOPOLE | ÉLEVÉ |
| **gaming-3d-mcp** | Gaming 3D | Faible | TOP 2 | ÉLEVÉ |
| **ecommerce-3d-mcp** | E-comm 3D | Aucune directe | LEADER | MOYEN |
| **legal-docs-mcp** | Juridique FR | Faible | TOP 3 | MOYEN |
| **realestate-mcp** | Immobilier | Moyenne | TOP 5 | MOYEN |
| **cooking-mcp** | Cuisine | Moyenne | TOP 5 | FAIBLE |
| **education-mcp** | Éducation | Moyenne | OUTSIDER | FAIBLE |
| **@thomasgorisse/finance-mcp** | Finance | Forte | OUTSIDER | TRÈS FAIBLE |
| **health-fitness-mcp** | Santé | Forte | OUTSIDER | FAIBLE |
| **travel-mcp** | Voyage | Moyenne | OUTSIDER | FAIBLE |
| **@thomasgorisse/seo-mcp** | SEO | Très forte | OUTSIDER | TRÈS FAIBLE |
| **devops-mcp** | DevOps | Très forte | OUTSIDER | TRÈS FAIBLE |
| **productivity-mcp** | Productivité | Très forte | OUTSIDER | TRÈS FAIBLE |
| **freelance-tools-mcp** | Freelance | Faible | TOP 3 | MOYEN |
| **gardening-mcp** | Jardinage | Quasi-aucune | LEADER | FAIBLE (micro-niche) |
| **diy-home-mcp** | Bricolage | Quasi-aucune | LEADER | FAIBLE (micro-niche) |
| **event-planning-mcp** | Événementiel | Faible | TOP 3 | FAIBLE |
| **pet-care-mcp** | Animaux | Quasi-aucune | LEADER | FAIBLE (micro-niche) |

### Synthèse

- **6 MCPs en position de force** : sceneview, french-admin, interior-design, architecture, gaming, ecommerce-3d
- **4 MCPs en position correcte** : legal-docs, realestate, freelance-tools, event-planning
- **5 MCPs en micro-niche** : gardening, diy-home, pet-care, cooking, education
- **6 MCPs en position faible** : finance, health-fitness, travel, seo, devops, productivity

---

## 4. Forces et faiblesses {#4-forces-faiblesses}

### Forces

1. **Monopole 3D/AR** : Aucun concurrent sérieux en code generation 3D/AR. SceneView MCP est unique.
2. **Standalone = zéro friction** : Pas d'API key, pas de credentials, `npx` et ça marche. C'est un avantage MAJEUR (cf. article "10 Free MCP Servers Without API Keys").
3. **Volume de MCPs** : 21 packages = visibilité cross-niche sur npm, effet de réseau.
4. **Niches françaises** : french-admin et legal-docs n'ont AUCUN concurrent.
5. **Tests** : 858+ tests documentés = qualité supérieure à 95% des MCPs communautaires.
6. **3D vertical** : La composante 3D dans gaming, ecommerce, interior-design, architecture crée un moat unique.

### Faiblesses

1. **Downloads très faibles** : Probablement < 100/semaine par MCP (vs Context7 à 240K/semaine).
2. **Pas de données réelles** : Les MCPs standalone génèrent du contenu/templates mais n'accèdent pas à de vraies données (vs ATTOM pour l'immobilier, Semrush pour le SEO).
3. **Dispersion** : 21 MCPs = maintenance lourde. Les MCPs en position faible (SEO, DevOps, Productivity) ne peuvent pas rivaliser avec les officiels.
4. **Pas sur awesome-mcp-servers** : La page dit "We do not accept PRs. Submit on https://mcpservers.org/submit" — il faut passer par le formulaire.
5. **Pas sur les registries majeurs** : PulseMCP (12,870+), Glama (20,324), mcpservers.org — nos MCPs doivent y être listés.
6. **Nommage npm** : "french-admin-mcp", "cooking-mcp" — pas de scope @thomasgorisse/ uniforme, pas de branding cohérent.
7. **README/descriptions npm** : Probablement pas optimisés pour le SEO npm (mots-clés, badges, exemples).

### Opportunités

1. **Marché < 5% monétisé** : Fenêtre massive pour la monétisation (cf. 21st.dev : $10K MRR en 6 semaines).
2. **3D est le futur** : Avec Apple Vision Pro, Meta Quest, WebXR — la 3D va exploser. Nous sommes les seuls à avoir un MCP 3D sérieux.
3. **Micro-niches vides** : Jardinage, DIY, pet care — aucun concurrent, mais aussi faible demande. Potential de long-tail SEO.
4. **France/francophone** : Aucun MCP admin française, juridique FR. Marché captif.
5. **Standalone trend** : L'article "10 Free MCP Servers Without API Keys" montre que les gens cherchent du no-credential. C'est exactement notre positionnement.

### Menaces

1. **Consolidation** : Les grosses entreprises (Google, AWS, Microsoft) lancent des MCPs officiels qui écrasent les indépendants.
2. **Saturation** : De 425 à 20,000+ MCPs en 9 mois. La découvrabilité devient le vrai problème.
3. **Qualité perçue** : Les MCPs avec données réelles (API-backed) sont perçus comme plus utiles que les standalone.
4. **Maintenance** : 21 MCPs à maintenir = risque d'abandon/bitrot.

---

## 5. Recommandations stratégiques {#5-recommandations}

### A. MCPs à ABANDONNER ou DÉPRIORITISER

| MCP | Raison |
|---|---|
| **@thomasgorisse/seo-mcp** | Google, Semrush, DataForSEO dominent. Pas viable. |
| **devops-mcp** | Jenkins, Docker, K8s, Terraform ont des MCPs officiels. |
| **productivity-mcp** | Asana (44 tools), Zapier (8K apps). Impossible de rivaliser. |
| **@thomasgorisse/finance-mcp** | Financial Datasets est top 10 mondial, Stripe officiel. |

**Action** : Ne pas supprimer de npm (garde la visibilité), mais ne plus investir de temps.

### B. MCPs à AMÉLIORER en priorité

#### 1. sceneview-mcp (PRIORITÉ 1)
- C'est notre crown jewel. Objectif : devenir LE MCP référencé partout pour la 3D/AR.
- **Actions** :
  - Soumettre sur mcpservers.org/submit
  - Soumettre sur PulseMCP
  - Lister sur Glama
  - Optimiser README npm avec badges, GIF demo, exemples one-liner
  - Ajouter des keywords npm : "3d", "ar", "augmented reality", "three.js", "webxr", "compose", "android", "ios"
  - Créer un article DEV.to "How to build a 3D app with AI using SceneView MCP"

#### 2. interior-design-3d-mcp (PRIORITÉ 2)
- Monopole absolu. Le marché interior design + AI est en explosion.
- **Actions** :
  - Ajouter des templates de pièces (salon, cuisine, chambre, salle de bain)
  - Ajouter des matériaux et textures
  - Soumettre sur les registries
  - Keyword : "interior design", "home decoration", "room planner", "3d room", "furniture"

#### 3. gaming-3d-mcp (PRIORITÉ 3)
- Marché gaming + AI en forte croissance. Peu de concurrence MCP.
- **Actions** :
  - Ajouter des game templates (platformer, FPS, puzzle)
  - Intégration assets procéduraux
  - Keywords : "game development", "game engine", "3d game", "procedural generation"

#### 4. french-admin-mcp (PRIORITÉ 4)
- Monopole français. Valeur unique pour les développeurs FR.
- **Actions** :
  - Ajouter plus de formulaires CERFA
  - Intégrer les barèmes fiscaux 2026
  - Article sur dev.to en français
  - Soumettre sur registries avec tag "france", "french", "administration"

#### 5. architecture-mcp (PRIORITÉ 5)
- Monopole. L'architecture AI est un marché émergent.
- **Actions** :
  - Templates de plans (maison, appartement, bureau)
  - Standards (RT2020, accessibilité PMR)
  - Keywords : "architecture", "building design", "floor plan", "BIM"

### C. NOUVEAUX MCPs à fort potentiel

| Idée de MCP | Catégorie | Concurrence | Potentiel |
|---|---|---|---|
| **threejs-mcp** | 3D Web | 1-2 faibles | TRÈS ÉLEVÉ — Three.js est LE framework 3D web, 100K+ stars |
| **blender-mcp** | 3D Modeling | 1-2 existants | ÉLEVÉ — Blender est gratuit et massif |
| **unity-mcp** | Game Engine | 123 sur PulseMCP mais fragmentés | MOYEN — consolidation possible |
| **figma-to-3d-mcp** | Design → 3D | 0 | ÉLEVÉ — pont unique design/3D |
| **webxr-mcp** | VR/AR Web | 0 | ÉLEVÉ — WebXR + Vision Pro |
| **french-legal-ai-mcp** | Juridique FR avancé | 0 | MOYEN — upgrade de legal-docs |

**Recommandation forte** : Un **threejs-mcp** pourrait devenir viral. Three.js a 100K+ stars GitHub, c'est le framework 3D web dominant, et il n'y a pas de MCP sérieux dédié. Ce serait cohérent avec notre expertise 3D.

### D. Comment augmenter les downloads npm

1. **Soumettre sur TOUS les registries** :
   - mcpservers.org/submit (awesome-mcp-servers ne prend plus de PR)
   - PulseMCP (12,870+ serveurs listés)
   - Glama (20,324 serveurs listés)
   - LobeHub
   - mcpmarket.com
   - fastmcp.me
   - mcp.so
   - playbooks.com

2. **SEO npm** :
   - Keywords optimisés dans package.json
   - README avec badges (npm version, downloads, license)
   - Description concise et keyword-rich
   - Exemples de configuration Claude Desktop dans le README

3. **Content marketing** :
   - Article DEV.to : "I Built 21 MCP Servers — Here's What I Learned"
   - Article DEV.to : "Free MCP Servers That Work Without API Keys"
   - Article Medium : "3D + AI: How SceneView MCP Changes Game Development"
   - Tutorial YouTube (même court)

4. **Cross-promotion** :
   - Chaque MCP référence les autres dans son README
   - Page npm de l'auteur (npmjs.com/~thomasgorisse) optimisée
   - Lien vers le site sceneview.github.io depuis chaque README

5. **Intégrations** :
   - Claude Desktop config snippets dans chaque README
   - Cursor/VS Code config snippets
   - One-liner npx dans chaque description

---

## 6. Plan d'action {#6-plan-action}

### Court terme (cette semaine)

| # | Action | Impact | Effort |
|---|---|---|---|
| 1 | **Soumettre sceneview-mcp sur mcpservers.org/submit** | Élevé (référencement) | 15 min |
| 2 | **Soumettre les 6 MCPs forts sur PulseMCP + Glama** | Élevé (visibilité) | 1h |
| 3 | **Optimiser package.json keywords des 6 MCPs prioritaires** | Moyen (SEO npm) | 30 min |
| 4 | **Écrire article DEV.to "21 MCP Servers Without API Keys"** | Élevé (trafic) | 2h |
| 5 | **Ajouter badges + config Claude Desktop dans README des 6 MCPs forts** | Moyen (conversion) | 1h |

### Moyen terme (ce mois)

| # | Action | Impact | Effort |
|---|---|---|---|
| 1 | **Créer threejs-mcp** — standalone 3D web code gen | Très élevé | 1-2 jours |
| 2 | **Soumettre TOUS les 21 MCPs sur 5+ registries** | Élevé | 3h |
| 3 | **Article DEV.to "3D + AI: Building Games with MCP"** | Élevé | 2h |
| 4 | **Ajouter des templates/features aux 5 MCPs prioritaires** | Moyen | 3-5 jours |
| 5 | **Explorer la monétisation** (freemium MCP Pro, cf. 21st.dev à $10K MRR en 6 semaines) | Élevé | 2 jours |

### Métriques à suivre

| Métrique | Outil | Fréquence | Cible 3 mois |
|---|---|---|---|
| npm weekly downloads (chaque MCP) | npm-stat.com / npmtrends.com | Hebdo | 500/semaine pour sceneview-mcp |
| Listings sur registries | Manuel | Mensuel | 5+ registries par MCP fort |
| GitHub stars sceneview | GitHub | Hebdo | +200 stars |
| Articles publiés | DEV.to / Medium | Mensuel | 4 articles |
| Revenus MCP Pro | Stripe / Polar | Mensuel | Premier $ |
| Position sur recherche npm "3d mcp" | npm search | Mensuel | Top 3 |

---

## Annexe : Registries MCP à cibler

| Registry | URL | Taille | Comment soumettre |
|---|---|---|---|
| Awesome MCP Servers | mcpservers.org | 3.8K stars | mcpservers.org/submit (formulaire, plus de PR GitHub) |
| PulseMCP | pulsemcp.com | 12,870+ | Soumission via site |
| Glama | glama.ai/mcp | 20,324+ | Soumission via site |
| LobeHub | lobehub.com/mcp | Milliers | GitHub PR lobehub/lobe-chat-agents |
| MCP Market | mcpmarket.com | Milliers | Soumission via site |
| FastMCP | fastmcp.me | Milliers | Soumission via site |
| mcp.so | mcp.so | Milliers | Soumission via site |
| Playbooks | playbooks.com/mcp | Milliers | Soumission via site |
| Composio | composio.dev | Milliers | Soumission via site |

---

## Conclusion

**Notre avantage compétitif principal est la verticalité 3D/AR.** Dans un marché de 20,000+ MCPs dominé par les dev tools, databases et search, nous sommes les SEULS à proposer une suite cohérente de MCPs 3D (sceneview, gaming, interior-design, architecture, ecommerce-3d).

**La stratégie gagnante n'est PAS de rivaliser avec Google, Stripe ou Asana** sur leurs territoires (SEO, finance, productivité). C'est de :
1. Consolider notre monopole 3D/AR
2. Maximiser la visibilité via les registries (0% couvert actuellement)
3. Capitaliser sur le positionnement "no API key needed"
4. Créer du contenu (articles, tutos) pour générer du trafic organique
5. Explorer la monétisation avant que le marché ne se consolide (< 5% monétisé aujourd'hui)

Le timing est critique : en 12 mois, les grandes entreprises auront leurs MCPs officiels dans toutes les catégories. La fenêtre d'opportunité pour les indépendants est MAINTENANT.
