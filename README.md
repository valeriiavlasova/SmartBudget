# SmartBudget

Aplikacja na Androida do zarządzania finansami osobistymi: wydatkami, budżetem, kontami i oszczędnościami.

## Funkcje

- Wydatki: dodawanie, edycja, usuwanie, wyszukiwanie i historia miesięczna.
- Budżet miesięczny, limity kategorii oraz powiadomienia o operacjach finansowych i limitach.
- Konta, dochody i przelewy między własnymi kontami.
- Cele oszczędnościowe i płatności cykliczne.
- Interaktywne wykresy oraz prognozy wydatków i oszczędzania.
- Eksport CSV/PDF oraz kopie zapasowe JSON z możliwością przywracania.
- Trzy języki, 13 motywów kolorystycznych oraz tryb jasny i ciemny.

## Uruchomienie

1. Otwórz projekt w Android Studio.
2. Zainstaluj Android SDK Platform 34 i wybierz JDK 17.
3. Wykonaj synchronizację Gradle.
4. Uruchom aplikację na urządzeniu lub emulatorze z Androidem 8.0 (API 26) lub nowszym.

Projekt zawiera Gradle Wrapper 8.7. Plik `local.properties` ze ścieżką do SDK jest tworzony lokalnie przez Android Studio.

## Budowanie i testy

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

Ostatnie polecenie wymaga uruchomionego emulatora lub podłączonego urządzenia. Na macOS/Linux użyj `./gradlew`. Pierwsza synchronizacja wymaga internetu do pobrania zależności.

## Technologie

Kotlin, XML Views, View Binding, ViewModel, LiveData, Room, Kotlin Coroutines, WorkManager, Material Components i MPAndroidChart.

## Dane

Dane finansowe są przechowywane lokalnie. Nowa instalacja tworzy przykłady obejmujące sześć miesięcy; aktualizacja aplikacji zachowuje istniejące dane. Operacje są rejestrowane ręcznie, bez integracji z bankiem i przeliczania walut. Kopie JSON nie są szyfrowane, a ich przywrócenie zastępuje bieżące dane.
