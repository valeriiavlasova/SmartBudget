# App update

Open this project in Android Studio: C:/Users/dmitr/Documents/Codex/AndroidProjects/finanseosobiste

- Top bar menu > Settings: English, Ukrainian or Polish; Ocean blue, Lavender or Coral. Choices persist after restart.
- Add purchase: choose the purchase date. Future dates are disabled. Older purchases belong to their original month; the current dashboard only lists this month.
- Charts: doughnut with a wrapping legend showing category, amount and percentage.
- Charts > Next month forecast: enter opening balance, income and planned spending in PLN, then Update forecast. The solid line is projected balance; the dashed line is cumulative spending. Both cash flows are spread evenly through next month. This is a scenario, not an automatic prediction from purchase history.
- Existing database records are preserved; custom purchase/category text is not translated.

Ideas for later: recurring bills with reminders, savings goals, month selector and search for purchase history, CSV export, undo deletion and editing existing purchases.


## Planner update
- Plans tab: weekly, monthly and yearly recurring payments with a due date and category. Due payments can be marked paid; this records a purchase on the due date and advances one occurrence. Overdue occurrences are handled one at a time. There are no bank transfers or automatic deductions.
- Monthly schedules preserve the chosen day (31 January -> 28 February -> 31 March). Annual leap-day schedules return to 29 February in leap years.
- Savings goals: set target and starting savings, add contributions, see progress and completion, remove a goal. Contributions do not count as expenses.
- Expenses: choose month/year at the top; tap a purchase to edit title, amount, category, note and date. Historical totals are compared to the current limit, not a stored historical budget.
- Ten additional themes: Amber, Rose, Indigo, Sky, Teal, Emerald, Plum, Cocoa, Slate, Crimson. Thirteen themes total.
- Database version 2 uses an additive migration. Existing expense/category/budget tables remain intact. Payment recording and schedule advancement are a single transaction guarded against duplicate confirmation.


## Chart and appearance refinement
- Settings > Dark mode toggles full dark surfaces, text and dialogs while retaining the chosen accent.
- Settings > Budget for future months stores a rule from next month onward. Later rules supersede it; earlier months retain the previous effective rule. Database v3 adds month_budgets without deleting existing data.
- Tap a doughnut slice to highlight it and show its category and amount in the centre. Tap outside to restore the total.
- Spending vs limits uses horizontal bars with numbered category labels and a full category key underneath. Height grows with the category count.
- The final chart is now a savings projection: choose a saved goal and monthly contribution to see 12 months of projected savings and the target line. It does not record contributions.
- Month selection uses month/year wheels. Historical budgets cannot be edited with the current-month button.
- Plans now has separate Payments and Goals tabs; delete actions are in a small overflow menu. Added emoji cues to navigation and plan cards.


## Currency and scrolling update
- Brand: SmartBudget. Core navigation and main actions are English in every language. This upgrade selects English once; Language still offers English, Ukrainian and Polish.
- Settings > Currency: PLN, UAH, EUR, USD, GBP, CZK and CHF. All amount formatting uses the selected code and language-specific number formatting. Numeric amounts are not converted; the settings dialog explains this before changing the accounting currency.
- Purchase button is shown on Expenses only. Chart scrolling has an additional 120dp footer; the expense list reserves 140dp beneath its last row.
- Background budget checks now use the effective monthly budget rules.


## Data tools
- Expenses: search title/note/localized category and filter by category, minimum and maximum amount within the selected month. The budget card remains the full month total; the list header shows the filtered total.
- Deleting a purchase shows an Undo snackbar after the database delete succeeds. Undo restores its original ID/date/category/note.
- Top bar > Files: CSV/PDF export of the selected month and active filters, JSON backup, or restore. Android's document picker controls where files are saved/opened.
- CSV uses UTF-8 BOM, quoted fields, decimal dots, ISO dates and explicit currency codes. Formula-like text is escaped for spreadsheet safety.
- PDF paginates and wraps long titles and notes.
- Backup v1 includes all six data tables, currency, language, theme, dark mode and forecast settings. Restore validates format, schema fields and references and replaces database records in one transaction. It asks for confirmation because it replaces existing data. Maximum backup/import size: 10 MiB, up to 100,000 rows per table. Files are plain JSON, not encrypted; save in a private location.


## Receipt scanning
- Scan receipt in the top bar or new-purchase dialog opens Camera / Choose photo.
- Camera uses the system camera and a FileProvider limited to cache/receipts; photo import uses the system image picker. No broad storage access is requested.
- Bundled ML Kit Latin text recognizer processes photos on the device. English/Polish receipts are the supported starting point. Ukrainian Cyrillic text can be incomplete and must be corrected manually.
- Suggested merchant title, a total from explicit total-label lines, and a valid nonfuture date prefill the purchase form. The OCR text becomes the editable note. Category remains a user choice. If no total is found, the amount stays blank.
- Every scan opens review before Save; mismatched receipt/app currency is shown and no currency conversion occurs.
- Camera capture URI survives activity recreation; recognition runs in the ViewModel. Photos are temporary cache items and are not included in JSON backups.
- Added parser tests for subtotal/cash/change exclusion, grouped comma amounts, missing totals and invalid/future dates.


## Money activity notifications
- Settings > Notifications: independent switches for budget warnings, expense additions/edits/deletions/restores, recorded recurring payments, and savings contributions. Preferences persist and are included in JSON backups.
- Money notifications use a separate Android channel at normal importance; budget warnings keep their existing channel. Notifications hide financial detail on the lock screen unless the device/user settings allow it.
- Events are emitted after successful database writes. Repeated confirmation of the same recurring payment does not emit a second payment notification.
- Android settings in the dialog opens the app notification controls. Android 13+ also requires notification permission; a blocked app/channel cannot be overridden by the in-app switches.
- Payments are manual records, not bank transfers. This feature does not monitor external bank transactions.


## Fresh Mint layout
- Rebuilt Expenses, Analytics and Savings goals to follow the Fresh Mint concept: custom line icons, compact bottom navigation, mint budget card with remaining balance, side-by-side actions, grouped transaction rows, white cards and lavender/green charts.
- Expenses shows three recent transactions initially. See all reveals the full list and the Filters action. Search always searches the full selected month. Tap the budget card to edit the current month; long-press a transaction for edit/delete actions.
- Analytics uses the selected month for its category chart and real six-month totals for the rounded bar chart. More analytics expands the existing category-limit and savings-forecast tools.
- Plans opens savings goals. Recurring payments expands below the goal cards.
- Tested on emulator-5554. Demonstration data for screenshot comparison exists only in that fresh emulator installation, not in the shipped database initializer.


## Complete colour palettes
- All 13 themes now define primary and secondary accents, card/button containers, progress tracks, logo colours and chart palettes in both light and dark mode.
- Removed fixed mint/lavender accents from UI paint attributes. Text and page surfaces use neutral colours; warning/error colours and user-selected category swatches retain their meaning.
- Theme changes still use the existing Settings selector and persist across restart. Component hierarchy, spacing, sizing and navigation are unchanged.


## Accounts, income and history forecast
- Top menu > Accounts & income opens account balances, account creation, income recording and internal transfers. Tap an account to edit its name and opening balance. Long-press an income/transfer to delete it; deleting a transfer removes both legs.
- The purchase form now includes an account. Add/edit/delete/undo naturally affect balances via a derived balance query, rather than a separately mutated total. Recurring payment records use Main account (ID 1).
- Schema v4 adds accounts/cash_entries and an expense accountId via migration 3→4. All legacy expenses are assigned to Main account; its opening balance starts at zero and can be set by the user. Existing migrations 1→2→3 are retained.
- Income, transfers and opening balances are stored in integer minor units; expense storage remains legacy REAL for compatibility, with each expense rounded to two decimals before integer balance aggregation. New purchase input accepts at most two decimal places.
- JSON backup format v2 includes both new tables. Restore accepts v1/v2, validates transfer pairs and references, and rolls back invalid imports. v1 imports create Main account and preserve old purchases.
- Charts > History-based forecast compares a trailing average (up to three prior full months) and current-month pace. It excludes the first recorded month from training because it may be partial. Backtests use only information available before each target/cutoff, and display MAE and sample counts. Sparse or incomplete history is explicitly identified.
- Separate FinanceViewModel/FinanceDao and a pure SpendingForecast module keep new responsibilities outside MainActivity/BudgetViewModel.
