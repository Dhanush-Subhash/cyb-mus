# cyb-mus Flutter

This is the Flutter migration of cyb-mus. It is intentionally separate from the original Android/Compose app while the migration is being verified.

## Run

Install Flutter 3.24+ and run:

```sh
flutter create .
flutter pub get
flutter run
```

The first Flutter pass includes:

- local audio scanning through MediaStore;
- local playback with `just_audio`;
- online search and streaming through the service used by the supplied `add this` code;
- the Library cloud toggle beside Reload;
- a player tab and dark cyber-styled Material 3 shell.

The supplied `add this` directory contains reusable Flutter services, but it is not a complete Flutter application and references feature folders that were not included. Those services will be migrated into this project incrementally once the base app is running.
