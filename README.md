# Print Bridge (Deep Link + ESC/POS)

This app handles HTTPS Android App Links like:

```
https://app.mycompany.com/orders/12345
```

When opened, it downloads the PDF from:

```
https://pos.therestsuites.com/getPDF/{pdf_id}
```

The `{pdf_id}` is taken from the deep link path segment after `/orders/`.

## Deep Link Verification

Host the following at:

```
https://app.mycompany.com/.well-known/assetlinks.json
```

Use the template in `docs/assetlinks.json` and replace the SHA-256 fingerprint with the app's signing certificate.

## Printing Flow

- The app auto-connects to the saved Bluetooth printer (paired device).
- It renders the first page of the PDF to a bitmap, converts it to ESC/POS raster data, then sends:
  1) Initial commands (if any)
  2) Raster image data
  3) Cutter commands
  4) Drawer commands

## Settings

The Advanced Settings screen includes:

- Print mode (Graphic/Text)
- Print width (mm)
- Print resolution (dpi)
- Initial ESC/POS commands
- Cutter ESC/POS commands
- Drawer ESC/POS commands

Commands are comma-separated hex bytes, e.g.:

```
1D,56,42,00
```
