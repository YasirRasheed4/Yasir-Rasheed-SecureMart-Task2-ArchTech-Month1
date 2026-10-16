# SecureMart

A mobile-only Android marketplace where registered users can **buy and sell** products, with a focus on
**data security and payment safety**. Built for Arch Technologies - Android App Development internship,
Month 1, Task 2 (E-Commerce Application).

Kotlin - Jetpack Compose (Material 3) - Room (SQLite) - fully offline, no Firebase or accounts needed.

## Features
- Register / log in (email + password)
- Browse products, search, filter by category
- Product details, quantity picker, add to cart
- Cart with live totals and stock checks
- Checkout: delivery details + card payment (demo gateway) -> order history
- **Sell**: list your own product with a photo, edit or remove it, see sales and revenue
- Profile with a summary of the security protections

## Security and payment safety
| Area | What the app does |
|---|---|
| Passwords | PBKDF2-HMAC-SHA256, 120,000 iterations, unique random salt per user, constant-time compare |
| Login abuse | Generic error message, account locks for 5 min after 5 wrong passwords, equal-time check for unknown emails |
| Sessions | Auto sign-out after 30 min away from the app |
| Card data | Validated (Luhn, expiry, CVV) and sent only to the payment gateway. Card number and CVV are **never stored**; the order keeps only brand + last 4 digits + payment reference |
| Payment design | App talks to a `PaymentGateway` interface; the demo `MockPaymentGateway` can be swapped for a real processor |
| Order integrity | Totals are recalculated from database prices; stock is decremented atomically inside a DB transaction; payment is voided if the order cannot be saved |
| Access control | Users can only edit/remove their own listings, cannot buy their own items, can only change their own cart |
| Input safety | All text is cleaned and length-limited; all SQL uses bound parameters |
| Photos | Resized and re-encoded, which strips EXIF/GPS metadata; stored in private app storage |
| Device | No INTERNET permission, backups disabled, cleartext traffic off, portrait only; optional `FLAG_SECURE` in `SecurityConfig` |

## Run it
1. Install Android Studio (JDK 17) and open the `SecureMart` folder.
2. Wait for Gradle sync, choose an emulator or phone (API 26+), press Run.

## Try it
1. **Create account** (e.g. password `Demo@1234`).
2. **Shop** tab: open a product, add to cart. **Cart** tab: change quantity, tap *Secure checkout*.
3. Checkout with test card **4242 4242 4242 4242**, any future expiry (e.g. 12 / 30), any 3-digit CVV.
   - Test card **4000 0000 0000 0002** is declined (shows the failure path).
4. **Orders** tab shows the order with `Visa ending 4242`.
5. **Sell** tab: *List a new product* (add a photo). Edit or remove it from the list.
6. To see the buyer side of your own listing, log out, create a second account and buy it, then log back in
   to the first account and open **Sell > Sales**.

## Structure
```
app/src/main/java/com/securemart/
  MainActivity.kt, SecureMartApp.kt   navigation + app shell
  MartViewModel.kt                    state and actions
  data/       Entities, DAOs, Repository (all rules), ImageStore, models
  security/   PasswordHasher, Validators, SessionManager, SecurityConfig
  payment/    PaymentGateway interface + MockPaymentGateway
  ui/         Auth, Home, ProductDetail, Cart, Checkout, Sell, ProductForm, Orders, Profile
```
