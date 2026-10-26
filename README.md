# Heimdall

A React + TypeScript frontend built with Vite, Tailwind CSS, Framer Motion, and GSAP.

The black landing page includes:

- An animated Tilted Grid Hero with ten supplied R2 images.
- An immersive fullscreen navigation with a clip-path reveal, keyboard focus trap, and Escape-to-close.
- A wheel-driven Perspective Tunnel Hero with pointer parallax.
- A responsive feature-card section.

## Local development

Requires Node.js 22.12+ and npm.

```sh
npm install
npm run dev
```

Open http://127.0.0.1:5174.

```sh
npm run build
npm run preview
```

The production build is written to `dist/`. Preview runs on port 4174.

## Project structure

- `src/App.tsx`: page composition and image configuration.
- `src/components/ui/`: reusable hero, navigation, and card components.
- `src/components/blocks/features-8.tsx`: feature section.
- `src/styles.css`: theme and page styles.

The `@/` alias resolves to `src/`. Images load from the supplied external CDNs. Animation components include reduced-motion handling.

## Optional backend proxy

The current landing page does not perform lead searches. For future integration, Vite forwards `/api` to `http://localhost:8080`. Copy `.env.example` to `.env` to override `API_TARGET`. Keep API credentials on the backend.

For static production hosting, configure a reverse proxy separately if `/api` is needed; Vite's development proxy is not included in the build.

The fullscreen navigation retains the supplied Northline demo copy and placeholder links. Replace these with real destinations before publishing as a production site.
