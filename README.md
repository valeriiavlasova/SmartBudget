# SmartBudget

Aplikacja na Androida do zarządzania finansami osobistymi: wydatkami, budżetem, kontami i oszczędnościami.

## Funkcje

- Wydatki z wyszukiwaniem, filtrowaniem i historią miesięczną.
- Budżet miesięczny, kategorie i powiadomienia o limitach.
- Konta, dochody i przelewy wewnętrzne.
- Cele oszczędnościowe i płatności cykliczne.
- Wykresy i prognoza wydatków z oceną błędu na danych historycznych.
- Skanowanie paragonów z weryfikacją rozpoznanych danych.
- Eksport CSV/PDF, kopie zapasowe JSON i ich przywracanie.
- Trzy języki, 13 motywów kolorystycznych, tryb jasny i ciemny.

Pełny skrócony opis: [Funkcje aplikacji](docs/FUNKCJE_PL.md).

## Uruchomienie

1. Zainstaluj Android Studio i Android SDK Platform 34.
2. Otwórz katalog projektu w Android Studio i wykonaj synchronizację Gradle.
3. Użyj JDK 17 lub zgodnego środowiska JBR z Android Studio.
4. Uruchom aplikację na urządzeniu lub emulatorze z Androidem 8.0 (API 26) lub nowszym.

Projekt zawiera Gradle Wrapper 8.7. Android Studio ustawia lokalną ścieżkę SDK w pliku `local.properties`, który nie jest przechowywany w repozytorium.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Na macOS/Linux użyj `./gradlew`. Pierwsza synchronizacja wymaga internetu do pobrania zależności z Google Maven, Maven Central i JitPack.

Testy urządzeniowe wymagają uruchomionego emulatora lub podłączonego urządzenia:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Technologie

Kotlin, XML Views, ViewBinding, MVVM, Room, LiveData, Kotlin Coroutines, WorkManager, ML Kit i MPAndroidChart.

## Informacje o danych

Operacje finansowe są rejestrowane ręcznie. Aplikacja nie wykonuje płatności bankowych ani konwersji walut. Kopie JSON nie są szyfrowane. Przy pierwszym uruchomieniu tworzony jest przykładowy budżet i demonstracyjne wydatki.

Przed udostępnieniem nie dodawaj do repozytorium kluczy podpisywania, rzeczywistych danych finansowych ani plików z danymi dostępowymi.
