# SmartBudget — skrócony wykaz funkcji

SmartBudget to aplikacja na Androida do ewidencji finansów osobistych, kontroli budżetu i planowania oszczędności.

## 1. Ewidencja wydatków — Expenses

- Dodawanie, edytowanie i usuwanie wydatków z możliwością cofnięcia usunięcia.
- Zapisywanie nazwy, kwoty, kategorii, notatki, daty i konta, z którego poniesiono wydatek.
- Historia miesięczna, wyszukiwanie oraz filtrowanie według kategorii i kwoty.
- Podgląd łącznych wydatków miesiąca oraz podsumowania przefiltrowanych zapisów.

## 2. Budżet i kategorie — Limits

- Całkowity budżet miesięczny, pozostała kwota i procent wykorzystania.
- Oddzielne limity kategorii, ich edycja oraz tworzenie własnych kategorii.
- Ostrzeżenia o zbliżaniu się do limitu i przekroczeniu budżetu.
- Ustalanie budżetu od następnego miesiąca z zachowaniem wcześniejszych reguł budżetowych.

## 3. Konta i dochody — Accounts & income

- Tworzenie kont oraz zmiana ich nazwy i salda początkowego.
- Rejestrowanie dochodów i przelewów między własnymi kontami.
- Automatyczne przeliczanie sald po operacjach finansowych.
- Podgląd ostatnich dochodów i przelewów oraz ich usuwanie po potwierdzeniu.
- Przelewy nie są traktowane jako wydatki i nie zmieniają łącznego salda wszystkich kont.

## 4. Analiza i prognozy — Charts

- Wykres pierścieniowy wydatków według kategorii z kwotami i udziałami procentowymi.
- Wykres wydatków z sześciu miesięcy: dotknięcie słupka pokazuje miesiąc i wydaną kwotę, również dla zerowych wydatków.
- Porównanie wydatków i limitów z nazwami kategorii bezpośrednio przy słupkach; długie nazwy mają kontrolowaną szerokość i zawijanie.
- Czytelny komunikat „Brak wydatków w tym miesiącu” przy pustym wykresie kategorii.
- Prognoza wydatków bieżącego miesiąca na podstawie średniej z poprzednich miesięcy i aktualnego tempa wydatków.
- Krótkie podsumowanie dwóch prognoz; przycisk „Dokładność” pokazuje błąd historyczny i liczbę sprawdzonych miesięcy.
- Symulacja oszczędzania przez 12 miesięcy dla wybranego celu i miesięcznej wpłaty.

## 5. Cele oszczędnościowe i płatności cykliczne — Plans

- Tworzenie celów oszczędnościowych, dodawanie wpłat i prezentacja postępu.
- Usuwanie celów po potwierdzeniu.
- Płatności tygodniowe, miesięczne i roczne z datą następnej płatności.
- Ręczne oznaczenie płatności jako opłaconej tworzy wydatek i przesuwa termin na kolejny okres.
- Ochrona przed podwójnym zapisaniem tej samej płatności oraz prawidłowa obsługa końca miesiąca i lat przestępnych.

## 6. Powiadomienia — Notifications

- Ostrzeżenia dotyczące limitów budżetowych.
- Powiadomienia o dodaniu, zmianie, usunięciu i przywróceniu wydatku, zapisaniu płatności cyklicznej, wpłacie na cel, dochodzie i przelewie.
- Oddzielne przełączniki typów powiadomień oraz przejście do ustawień systemowych Androida.
- Okresowe sprawdzanie limitów budżetowych w tle.

## 7. Eksport i kopie zapasowe — Files

- Eksport wydatków wybranego miesiąca do CSV i PDF z uwzględnieniem filtrów.
- Pełna kopia danych finansowych i ustawień w formacie JSON.
- Przywracanie danych z walidacją pliku i obsługą starszego formatu kopii.

## 8. Ustawienia i przechowywanie danych — Settings

- Język ukraiński, polski i angielski; część nawigacji pozostaje w języku angielskim.
- 13 motywów kolorystycznych oraz tryb jasny i ciemny.
- Wybór waluty: PLN, UAH, EUR, USD, GBP, CZK, CHF.
- Lokalna baza danych, podstawowa ewidencja bez dostępu do internetu oraz zachowanie danych po ponownym uruchomieniu.

## 9. Narzędzia i technologie

| Narzędzie / technologia | Zastosowanie w projekcie |
| --- | --- |
| Android Studio | Środowisko do tworzenia, uruchamiania i debugowania aplikacji. |
| Kotlin 1.9.24 | Główny język programowania aplikacji i jej logiki finansowej. |
| XML i View Binding | Definicja ekranów, motywów i zasobów oraz dostęp do widoków z kodu Kotlin. |
| Android SDK | Kompilacja i docelowe API: 34; minimalna wersja systemu: Android 8.0, API 26. |
| Gradle 8.7, Android Gradle Plugin 8.5.2 | Zarządzanie zależnościami, kompilacja, testy i budowanie pliku APK. Projekt korzysta z ustawień zgodności Java/JVM 17. |
| KSP 1.9.24-1.0.20 | Generowanie kodu obsługi bazy danych Room podczas kompilacji. |
| Android Emulator i ADB | Uruchamianie aplikacji na emulatorze, instalowanie APK i diagnostyka. |
| Git i GitHub | Kontrola wersji i przechowywanie kodu źródłowego projektu. |

## 10. Najważniejsze biblioteki

| Biblioteka | Rola w aplikacji |
| --- | --- |
| AndroidX Core KTX 1.13.1 i AppCompat 1.7.0 | Podstawowe rozszerzenia Kotlin oraz obsługa komponentów i motywów Androida. |
| Material Components 1.12.0 | Przyciski, pola formularzy, okna dialogowe i inne elementy interfejsu. |
| Fragment KTX 1.7.1, RecyclerView 1.3.2, ConstraintLayout 2.1.4 | Ekrany aplikacji, listy danych i rozmieszczanie elementów. |
| Lifecycle 2.7.0: ViewModel i LiveData | Przechowywanie stanu ekranów oraz obserwowanie zmian danych. |
| Kotlin Coroutines 1.8.1 | Asynchroniczne wykonywanie operacji, m.in. odczytu i zapisu danych. |
| Room 2.6.1 | Lokalna baza SQLite: encje, zapytania DAO, transakcje i migracje. |
| WorkManager 2.9.0 | Okresowe sprawdzanie wykorzystania limitów w tle. |
| MPAndroidChart 3.1.0 | Wykresy wydatków, limitów i scenariuszy oszczędzania. |
| JUnit 4.13.2, Coroutines Test 1.8.1, AndroidX Test JUnit 1.3.0, Espresso 3.7.0 i Room Testing 2.6.1 | Testy logiki, interfejsu oraz działania i migracji bazy danych. |

## 11. Jak zrealizowano aplikację

- **Architektura:** zastosowano podział zbliżony do MVVM. `MainActivity` i fragmenty obsługują interfejs, a klasy ViewModel przygotowują dane dla ekranów. `BudgetRepository` pośredniczy w dostępie do części danych; pozostałe operacje korzystają bezpośrednio z DAO. LiveData przekazuje zmiany do interfejsu, a korutyny wykonują operacje poza głównym wątkiem.
- **Przechowywanie danych:** baza Room zawiera osiem tabel: kategorie, wydatki, konfigurację budżetu, budżety miesięczne, płatności cykliczne, cele oszczędnościowe, konta i operacje na kontach. Migracje pozwalają aktualizować strukturę bazy z zachowaniem zapisanych danych.
- **Operacje finansowe:** saldo konta jest obliczane z salda początkowego, dochodów, przelewów i wydatków. Przelew tworzy dwa powiązane zapisy w jednej transakcji bazy danych. Dochody, przelewy i salda początkowe są przechowywane jako całkowite wartości najmniejszych jednostek waluty, np. groszy; wydatki i część danych budżetowych korzystają z typu `Double`.
- **Budżety i powiadomienia:** aplikacja porównuje sumę wydatków z limitem. Próg ostrzegawczy wynosi 80%, a osiągnięcie 100% uruchamia komunikat o wykorzystaniu limitu. Powiadomienia korzystają z kanałów Androida i `NotificationCompat`; w Androidzie 13 lub nowszym wymagają zgody użytkownika. WorkManager zleca sprawdzanie limitów co 12 godzin, przy czym rzeczywisty termin zależy od systemu.
- **Analiza i prognozy:** dane z bazy są agregowane według miesięcy i kategorii, a następnie przedstawiane na wykresach. `SpendingForecast` oblicza dwa oszacowania: średnią z maksymalnie trzech poprzednich pełnych miesięcy oraz prognozę według wzoru „wydatki do dziś / liczba dni, które upłynęły × liczba dni miesiąca”. Pierwszy miesiąc ewidencji jest pomijany w średniej jako potencjalnie niepełny. Trafność jest oceniana na wcześniejszych miesiącach przez średni błąd bezwzględny (MAE). Są to obliczenia statystyczne, bez modelu uczenia maszynowego.
- **Cele i płatności cykliczne:** postęp celu wynika z zapisanych wpłat. Symulacja pokazuje kwotę po kolejnych miesiącach przy stałej wpłacie. `PaymentSchedule` wyznacza kolejne terminy z uwzględnieniem długości miesięcy i lat przestępnych; zapis opłaconej płatności i przesunięcie jej terminu odbywają się w jednej transakcji z ochroną przed duplikatem.
- **Pliki i kopie zapasowe:** CSV jest tworzony jako tekst UTF-8, PDF przez systemową klasę `PdfDocument`, a JSON przez `JSONObject` i `JSONArray`. Systemowy wybór plików Androida umożliwia wskazanie miejsca zapisu i odczytu. Import sprawdza poprawność danych i ich powiązań, a następnie przywraca je w transakcji bazy danych.
- **Personalizacja:** preferencje języka, waluty, kolorów i powiadomień są zapisywane w `SharedPreferences`. Tłumaczenia korzystają z zasobów językowych Androida, a wygląd z motywów i atrybutów kolorystycznych.
- **Weryfikacja:** testy JUnit sprawdzają logikę obliczeń, a testy AndroidX i Espresso — bazę danych, motywy oraz działanie ekranów i wybór miesięcy na wykresie. Kontrola statyczna korzysta z Android Lint.

## Dane demonstracyjne

Przykłady obejmują sześć miesięcy zakupów i dochodów, trzy konta, przelewy, sześć kategorii, siedem reguł budżetu miesięcznego (w tym na następny miesiąc), trzy cele oszczędnościowe i cztery płatności cykliczne. Wydatki mają notatkę „Przykład demonstracyjny”.

Nowa instalacja tworzy przykłady automatycznie. Aktualizacja APK zachowuje dotychczasowe dane. Dołączony plik `examples/SmartBudget-demo.json` zawiera 127 przykładowych wydatków od czerwca do listopada 2026 r. Można go zaimportować przez Files → Continue → Przywróć kopię; import zastępuje bieżące dane i ustawienia.

## Ważne informacje

- Operacje są rejestrowane ręcznie: aplikacja nie oferuje integracji z bankiem, automatycznych płatności ani synchronizacji w chmurze.
- Zmiana waluty nie przelicza kwot. Wszystkie konta korzystają ze wspólnej waluty ewidencyjnej.
- Wpłaty na cele są ewidencjonowane oddzielnie i nie są automatycznie odejmowane od sald kont. Płatności cykliczne korzystają z konta Main account.
- Prognozy są szacunkami, a nie gwarancjami.
- Przypomnienia przed terminem płatności nie zostały jeszcze wdrożone.
- Kopia JSON nie jest szyfrowana; jej przywrócenie zastępuje bieżące dane.
