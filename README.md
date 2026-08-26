# weightop

Comment system with different deploy variants: from simple app to high-load app

> 🌐 **Languages:** [🇬🇧 English](README.md) | [🇷🇺 Русский](README.ru.md)

---

## Project Goal

The goal of this project is to develop a comment system that can operate under different load levels. Each version builds upon the previous one, adding optimizations and architectural changes to handle increasing load.

| Version | Load Level | Description | Documentation |
|---------|------------|-------------|---------------|
| **v1-simple** | Low (50–100 RPS) | MVP with basic CRUD, flat comments, single PostgreSQL | [README](v1-simple/README.md) |
| **v2-inter** | Medium (500–1000 RPS) | Adds caching, read replicas, keyset pagination | [README](v2-inter/README.md) |
| **v3-highload** | High (5000+ RPS) | Sharding, message queues, denormalization, CDN | [README](v3-highload/README.md) |

---

## Repository Structure

weightop/
├── README.md           
├── README.ru.md        
├── v1-simple/
│   ├── README.md       
│   ├── src/
│   ├── build.gradle
│   └── ...
├── v2-inter/
│   ├── README.md       
│   └── ...
└── v3-highload/
    ├── README.md       
    └── ...


---

## Quick Start

Each version has its own documentation and setup instructions:

- **[v1-simple](v1-simple/README.md)** — start here if you need a basic comment system.
- **[v2-inter](v2-inter/README.md)** — when you outgrow v1 and need better performance.
- **[v3-highload](v3-highload/README.md)** — for large-scale distributed deployments.

---

## License

MIT