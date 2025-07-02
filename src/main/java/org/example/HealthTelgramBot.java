package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class HealthTelgramBot extends TelegramLongPollingBot {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    // 3 allgemein funktion
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void send(SendMessage message) {
        if (message.getText() == null || message.getText().isEmpty()) {
            System.err.println("Message text is null or empty");
            return;
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Update update) {
        if (!update.getMessage().hasText()) {
            SendMessage reply = new SendMessage();
            reply.setChatId(update.getMessage().getChatId().toString());
            reply.setText("❗Bitte , senden Sie Richte message");
            send(reply);
            return;
        }
        String chatId = update.getMessage().getChatId().toString();
        String message = update.getMessage().getText().trim();

        //UserData user = users.computeIfAbsent(chatId, id -> new UserData());

        UserData user = DBHelper.loadUserData(chatId);
        if (user == null) {
            user = new UserData();
            DBHelper.saveUserData(chatId, user);
        }

        if (message.startsWith("/start")) {
            sendWelcomeMessage(chatId);
        } else if (message.startsWith("Gewicht speichern")) {
            handleWeightInput(chatId, message, user);
        } else {
            handleRegularMessage(chatId, message, user);
        }
    }

    // all handel callbackData
    private void handleCallbackQuery(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String callbackData = update.getCallbackQuery().getData();

        //UserData user = users.computeIfAbsent(chatId, id -> new UserData());
        UserData user = DBHelper.loadUserData(chatId);
        if (user == null) {
            user = new UserData();
            DBHelper.saveUserData(chatId, user);
        }

        switch (callbackData) {
            case "BMI":
                user.step = 1;
                sendGenderSelection(chatId);
                break;
            case "MALE":
            case "FEMALE":
                handleGenderSelection(chatId, callbackData, user);
                break;
            case "REMINDER_ON":
                handleReminderSubscription(chatId, user, true);
                break;
            case "REMINDER_OFF":
                handleReminderSubscription(chatId, user, false);
                break;
            case "EXERCISE":
                sendExerciseMenu(chatId);
                break;
            case "TIME_15":
            case "TIME_30":
            case "TIME_60":
                handleTimeSelection(chatId, callbackData, user);
                break;
            case "CARDIO":
                sendCardioWorkout(chatId, user);
                break;
            case "STRENGTH":
                sendSTRENGTHWorkout(chatId, user);
                break;
            case "FLEXIBILITY":
                sendFLEXIBILITYWorkout(chatId, user);
                break;
            case "ACT_LOW":
            case "ACT_MED":
            case "ACT_HIGH":
                handleActivitySelection(chatId, callbackData, user);
                break;
            case "BACK_TO_REPORT":
                SendMessage report = new SendMessage();
                report.setChatId(chatId);
                report.setText(getCompleteHealthReport(user));
                report.setParseMode("Markdown");
                send(report);
                break;
            case "SHOW_PROGRESS":
                showProgressChart(chatId);
                break;
                case "Rezept":
                getMahlzeiten(user, chatId);
                break;
            case "BACK_TO_MAIN":
                sendWelcomeMessage(chatId);
                break;
        }
    }

    // erst funktion zeigen
    private void sendWelcomeMessage(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("✨ *Willkommen beim Fitness-Bot!* ✨\n\n"
                + "Ich kann Ihnen helfen mit:\n"
                + "• BMI-Berechnung\n"
                + "• Personalisierten Trainingsplänen\n"
                + "• Ernährungsempfehlungen\n"
                + "• Fortschrittsverfolgung\n\n"
                + "Wählen Sie eine Option: 👇");
        message.setParseMode("Markdown");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("BMI berechnen 🏋️‍♂️")
                        .callbackData("BMI")
                        .build()));

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("Heutiges Training 💪")
                        .callbackData("EXERCISE")
                        .build()));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        send(message);
    }

    // für Geschlecht wählen
    private void sendGenderSelection(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👤 *Wählen Sie Ihr Geschlecht:*");
        message.setParseMode("Markdown");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("👨 Männlich")
                        .callbackData("MALE")
                        .build()));

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("👩 Weiblich")
                        .callbackData("FEMALE")
                        .build()));

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("Zurück zum Hauptmenü")
                        .callbackData("BACK_TO_MAIN")
                        .build()));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        send(message);
    }

    // für Alt schreiben
    private void handleGenderSelection(String chatId, String gender, UserData user) {
        if (Objects.equals(gender, "MALE")){
            user.gender = "Männlich";
        }else {
            user.gender = "Weiblich";
        }
        user.step = 2;
        DBHelper.saveUserData(chatId, user);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎂 *Bitte geben Sie Ihr Alter an:*\n(z.B. 25)");
        message.setParseMode("Markdown");
        send(message);
    }

    //für Gewicht,Größe,ActivityLevel
    private void handleRegularMessage(String chatId, String message, UserData user) {
        SendMessage reply = new SendMessage();
        reply.setChatId(chatId);
        reply.setParseMode("Markdown");

        switch (user.step) {
            case 2:
                try {
                    user.age = Integer.parseInt(message);
                    user.step = 3;
                    DBHelper.saveUserData(chatId, user);
                    reply.setText("⚖️ *Bitte geben Sie Ihr Gewicht in kg an:*\n(z.B. 70)");
                } catch (NumberFormatException e) {
                    reply.setText("❗ Bitte geben Sie ein gültiges Alter ein (nur Zahlen)");
                }
                send(reply);
                break;
            case 3:
                try {
                    user.weight = Double.parseDouble(message);
                    handleWeightInput(chatId,message,user);
                    user.step = 4;
                    DBHelper.saveUserData(chatId, user);
                    reply.setText("📏 *Bitte geben Sie Ihre Größe in cm an:*\n(z.B. 170)");
                } catch (NumberFormatException e) {
                    reply.setText("❗ Bitte geben Sie ein gültiges Gewicht ein (nur Zahlen)");
                }
                send(reply);
                break;
            case 4:
                try {
                    user.height = Double.parseDouble(message);
                    user.step = 5;
                    DBHelper.saveUserData(chatId, user);
                    handleActivityLevel(chatId);
                } catch (NumberFormatException e) {
                    send(new SendMessage(chatId, "❗ Bitte gib eine gültige Körpergröße ein (nur Zahlen)."));
                }
                break;

            default:
                reply.setText("❓ Ich habe Ihre Eingabe nicht verstanden. Bitte verwenden Sie die Buttons oder /start");
                send(reply);
        }
        DBHelper.saveUserData(chatId, user);
    }

    private void sendWorkoutMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        //message.setParseMode("Markdown");
        message.setParseMode(null);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(Collections.singletonList(
                Collections.singletonList(
                        InlineKeyboardButton.builder()
                                .text("Zurück zum Hauptmenü")
                                .callbackData("BACK_TO_MAIN")
                                .build()
                )
        ));
        message.setReplyMarkup(keyboard);

        send(message);
    }

    //calculateTDEE und calculateBMR und BMI

    private double calculateBMI(double heightCm, double weightKg) {
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    //(Basal Metabolic Rate) هو عدد السعرات الحرارية التي يحتاجها الجسم لأداء الوظائف الأساسية
    private double calculateBMR(UserData user) {
        if (user.gender.equals("MALE")) {
            return 88.362 + (13.397 * user.weight) + (4.799 * user.height) - (5.677 * user.age);
        } else {
            return 447.593 + (9.247 * user.weight) + (3.098 * user.height) - (4.330 * user.age);
        }
    }

    //(Total Daily Energy Expenditure) هو إجمالي السعرات الحرارية التي يحرقها الجسم في
    private double calculateTDEE(UserData user) {
        double bmr = calculateBMR(user);
        double activityFactor;

        switch(user.activityLevel) {
            case "ACT_LOW": activityFactor = 1.375; break;  // خفيف
            case "ACT_MED": activityFactor = 1.55; break;   // متوسط
            case "ACT_HIGH": activityFactor = 1.725; break; // عالي
            default: activityFactor = 1.2;                  // خامل
        }

        return bmr * activityFactor;
    }

    //ideal Weight
    private double calculateIdealWeight(UserData user) {
        if (user.gender.equals("MALE")) {
            return 50 + 0.9 * (user.height - 152);
        } else {
            return 45.5 + 0.9 * (user.height - 152);
        }
    }

    //für tipps essen und Report

    private String getMealOptions(String mealType, String goal) {
        StringBuilder options = new StringBuilder();

        switch(mealType) {
            case "breakfast":
                if (goal.equals("LOSE")) {
                    // Option 1
                    options.append("1. 🥣 Haferflocken (50g) mit Magermilch (200ml) und Heidelbeeren (100g)\n");
                    options.append("   - 180 kcal (Hafer) + 90 kcal (Milch) + 57 kcal (Beeren)\n");
                    options.append("   - Makros: P:12g / C:52g / F:5g\n");
                    options.append("   🔹 Gesamt: 327 kcal\n\n");

                    // Option 2
                    options.append("2. 🍳 2 Eier (100g) mit Vollkorntoast (40g) und Avocado (30g)\n");
                    options.append("   - 143 kcal (Eier) + 106 kcal (Toast) + 48 kcal (Avocado)\n");
                    options.append("   - Makros: P:16g / C:22g / F:14g\n");
                    options.append("   🔹 Gesamt: 297 kcal\n\n");

                    // Option 3
                    options.append("3. 🥛 Griechischer Joghurt (150g) mit Mandeln (20g) und Chiasamen (10g)\n");
                    options.append("   - 150 kcal (Joghurt) + 116 kcal (Mandeln) + 49 kcal (Chia)\n");
                    options.append("   - Makros: P:18g / C:12g / F:16g\n");
                    options.append("   🔹 Gesamt: 315 kcal\n");
                }
                else if (goal.equals("GAIN")) {
                    // Option 1
                    options.append("1. 🥞 Protein-Pfannkuchen (3 Stück) mit Erdnussbutter (30g) und Banane (120g)\n");
                    options.append("   - 330 kcal (Pfannkuchen) + 180 kcal (Erdnussbutter) + 107 kcal (Banane)\n");
                    options.append("   - Makros: P:28g / C:72g / F:18g\n");
                    options.append("   🔹 Gesamt: 617 kcal\n\n");

                    // Option 2
                    options.append("2. 🧀 Omelett aus 3 Eiern mit Käse (50g) und Vollkorntoast (2 Scheiben)\n");
                    options.append("   - 215 kcal (Eier) + 200 kcal (Käse) + 212 kcal (Toast)\n");
                    options.append("   - Makros: P:42g / C:34g / F:28g\n");
                    options.append("   🔹 Gesamt: 627 kcal\n\n");

                    // Option 3
                    options.append("3. 🥣 Müsli (80g) mit Vollmilch (300ml) und getrockneten Früchten (30g)\n");
                    options.append("   - 288 kcal (Müsli) + 195 kcal (Milch) + 90 kcal (Früchte)\n");
                    options.append("   - Makros: P:18g / C:102g / F:12g\n");
                    options.append("   🔹 Gesamt: 573 kcal\n");
                }
                else {
                    // MAINTAIN
                    // Option 1
                    options.append("1. 🍌 Haferflocken (40g) mit Milch (250ml) und Apfel (1 mittelgroß)\n");
                    options.append("   - 144 kcal (Hafer) + 125 kcal (Milch) + 95 kcal (Apfel)\n");
                    options.append("   - Makros: P:10g / C:62g / F:6g\n");
                    options.append("   🔹 Gesamt: 364 kcal\n\n");

                    // Option 2
                    options.append("2. 🥑 2 Eier mit Vollkorntoast (1 Scheibe) und Tomaten (100g)\n");
                    options.append("   - 143 kcal (Eier) + 106 kcal (Toast) + 18 kcal (Tomaten)\n");
                    options.append("   - Makros: P:15g / C:23g / F:10g\n");
                    options.append("   🔹 Gesamt: 267 kcal\n\n");

                    // Option 3
                    options.append("3. 🍓 Quark (200g) mit Leinsamen (10g) und Beeren (100g)\n");
                    options.append("   - 200 kcal (Quark) + 53 kcal (Leinsamen) + 57 kcal (Beeren)\n");
                    options.append("   - Makros: P:28g / C:20g / F:10g\n");
                    options.append("   🔹 Gesamt: 310 kcal\n");
                }
                break;

            case "lunch":
                if (goal.equals("LOSE")) {
                    // Option 1
                    options.append("1. 🍗 Hähnchenbrust (150g) mit Brokkoli (200g) und Quinoa (50g roh)\n");
                    options.append("   - 165 kcal (Hähnchen) + 70 kcal (Brokkoli) + 180 kcal (Quinoa)\n");
                    options.append("   - Makros: P:46g / C:39g / F:6g\n");
                    options.append("   🔹 Gesamt: 415 kcal\n\n");

                    // Option 2
                    options.append("2. 🐟 Lachsfilet (120g) mit Spargel (150g) und Süßkartoffel (100g)\n");
                    options.append("   - 240 kcal (Lachs) + 45 kcal (Spargel) + 90 kcal (Süßkartoffel)\n");
                    options.append("   - Makros: P:30g / C:32g / F:12g\n");
                    options.append("   🔹 Gesamt: 375 kcal\n\n");

                    // Option 3
                    options.append("3. 🦃 Putenstreifen (100g) mit Vollkornnudeln (60g roh) und Tomatensauce\n");
                    options.append("   - 130 kcal (Pute) + 210 kcal (Nudeln) + 50 kcal (Sauce)\n");
                    options.append("   - Makros: P:32g / C:42g / F:4g\n");
                    options.append("   🔹 Gesamt: 390 kcal\n");
                }
                else if (goal.equals("GAIN")) {
                    // Option 1
                    options.append("1. 🥩 Rindfleisch (200g) mit Reis (100g roh) und Gemüsemischung (150g)\n");
                    options.append("   - 440 kcal (Rind) + 350 kcal (Reis) + 75 kcal (Gemüse)\n");
                    options.append("   - Makros: P:52g / C:85g / F:20g\n");
                    options.append("   🔹 Gesamt: 865 kcal\n\n");

                    // Option 2
                    options.append("2. 🐠 Lachs (180g) mit Kartoffelpüree (200g) und grünen Bohnen (100g)\n");
                    options.append("   - 360 kcal (Lachs) + 220 kcal (Kartoffeln) + 35 kcal (Bohnen)\n");
                    options.append("   - Makros: P:42g / C:52g / F:18g\n");
                    options.append("   🔹 Gesamt: 615 kcal\n\n");

                    // Option 3
                    options.append("3. 🍛 Hähnchencurry (150g Hähnchen) mit Basmatireis (120g roh)\n");
                    options.append("   - 165 kcal (Hähnchen) + 420 kcal (Reis) + 150 kcal (Sauce)\n");
                    options.append("   - Makros: P:45g / C:92g / F:12g\n");
                    options.append("   🔹 Gesamt: 735 kcal\n");
                }
                else {
                    // MAINTAIN
                    // Option 1
                    options.append("1. 🦆 Truthahnbrust (160g) mit Braunem Reis (80g roh) und Gemüse (200g)\n");
                    options.append("   - 176 kcal (Truthahn) + 280 kcal (Reis) + 100 kcal (Gemüse)\n");
                    options.append("   - Makros: P:48g / C:62g / F:8g\n");
                    options.append("   🔹 Gesamt: 556 kcal\n\n");

                    // Option 2
                    options.append("2. 🐟 Forelle (150g) mit Kartoffeln (150g) und Salat (100g)\n");
                    options.append("   - 300 kcal (Forelle) + 165 kcal (Kartoffeln) + 50 kcal (Salat)\n");
                    options.append("   - Makros: P:38g / C:42g / F:12g\n");
                    options.append("   🔹 Gesamt: 515 kcal\n\n");

                    // Option 3
                    options.append("3. 🧈 Tofu (120g) mit Quinoa (60g roh) und Ofengemüse (200g)\n");
                    options.append("   - 144 kcal (Tofu) + 216 kcal (Quinoa) + 140 kcal (Gemüse)\n");
                    options.append("   - Makros: P:30g / C:62g / F:14g\n");
                    options.append("   🔹 Gesamt: 500 kcal\n");
                }
                break;

            case "dinner":
                if (goal.equals("LOSE")) {
                    // Option 1
                    options.append("1. 🥗 Großer Salat mit Hähnchen (100g) und fettarmem Dressing (20g)\n");
                    options.append("   - 110 kcal (Hähnchen) + 50 kcal (Salat) + 30 kcal (Dressing)\n");
                    options.append("   - Makros: P:24g / C:10g / F:4g\n");
                    options.append("   🔹 Gesamt: 190 kcal\n\n");

                    // Option 2
                    options.append("2. 🍲 Gemüsesuppe (300g) mit magerem Rindfleisch (80g)\n");
                    options.append("   - 150 kcal (Suppe) + 176 kcal (Rind)\n");
                    options.append("   - Makros: P:28g / C:18g / F:8g\n");
                    options.append("   🔹 Gesamt: 326 kcal\n\n");

                    // Option 3
                    options.append("3. 🥦 Gebackener Lachs (100g) mit Blumenkohlreis (200g)\n");
                    options.append("   - 200 kcal (Lachs) + 100 kcal (Blumenkohl)\n");
                    options.append("   - Makros: P:26g / C:12g / F:10g\n");
                    options.append("   🔹 Gesamt: 300 kcal\n");
                }
                else if (goal.equals("GAIN")) {
                    // Option 1
                    options.append("1. 🍝 Vollkornpasta (100g roh) mit Hackfleischsauce (150g) und Käse (30g)\n");
                    options.append("   - 350 kcal (Pasta) + 330 kcal (Hack) + 120 kcal (Käse)\n");
                    options.append("   - Makros: P:42g / C:78g / F:24g\n");
                    options.append("   🔹 Gesamt: 800 kcal\n\n");

                    // Option 2
                    options.append("2. 🍗 Hähnchenkeule (200g) mit Ofenkartoffeln (200g) und Sauerrahm (30g)\n");
                    options.append("   - 440 kcal (Hähnchen) + 220 kcal (Kartoffeln) + 60 kcal (Sauerrahm)\n");
                    options.append("   - Makros: P:48g / C:52g / F:22g\n");
                    options.append("   🔹 Gesamt: 720 kcal\n\n");

                    // Option 3
                    options.append("3. 🥘 Linseneintopf (300g) mit Würstchen (100g) und Brot (50g)\n");
                    options.append("   - 330 kcal (Linsen) + 300 kcal (Wurst) + 133 kcal (Brot)\n");
                    options.append("   - Makros: P:42g / C:72g / F:18g\n");
                    options.append("   🔹 Gesamt: 763 kcal\n");
                }
                else {
                    // MAINTAIN
                    // Option 1
                    options.append("1. 🥣 Gemüsesuppe (200g) mit Vollkornbrot (1 Scheibe)\n");
                    options.append("   - 150 kcal (Suppe) + 106 kcal (Brot)\n");
                    options.append("   - Makros: P:10g / C:32g / F:4g\n");
                    options.append("   🔹 Gesamt: 256 kcal\n\n");

                    // Option 2
                    options.append("2. 🐟 Gebackener Fisch (150g) mit gedünstetem Gemüse (200g)\n");
                    options.append("   - 200 kcal (Fisch) + 100 kcal (Gemüse)\n");
                    options.append("   - Makros: P:32g / C:20g / F:8g\n");
                    options.append("   🔹 Gesamt: 300 kcal\n\n");

                    // Option 3
                    options.append("3. 🥗 Hühnchensalat (120g Hühnchen) mit Avocado (50g) und Dressing (20g)\n");
                    options.append("   - 132 kcal (Hühnchen) + 80 kcal (Avocado) + 50 kcal (Dressing)\n");
                    options.append("   - Makros: P:26g / C:12g / F:16g\n");
                    options.append("   🔹 Gesamt: 262 kcal\n");
                }
                break;

            case "snack":

                int multiplier = goal.equals("GAIN") ? 2 : 1;

                // Option 1
                options.append("1. 🍏 Apfel (80g) mit Mandelbutter ("+(10*multiplier)+"g)\n");
                options.append("   - 42 kcal (Apfel) + "+(60*multiplier)+" kcal (Mandelbutter)\n");
                options.append("   - Makros: P:"+(2*multiplier)+"g / C:"+(10*multiplier)+"g / F:"+(5*multiplier)+"g\n");
                options.append("   🔹 Gesamt: "+(42+(60*multiplier))+" kcal\n\n");

                // Option 2
                options.append("2. 🥚 Hartgekochtes Ei + Gurkensticks ("+(50*multiplier)+"g)\n");
                options.append("   - 70 kcal (Ei) + "+(8*multiplier)+" kcal (Gurke)\n");
                options.append("   - Makros: P:"+(6*multiplier)+"g / C:"+(1*multiplier)+"g / F:"+(5*multiplier)+"g\n");
                options.append("   🔹 Gesamt: "+(70+(8*multiplier))+" kcal\n\n");

                // Option 3
                options.append("3. 🧀 Magerquark (100g) mit Beeren ("+(50*multiplier)+"g)\n");
                options.append("   - 70 kcal (Quark) + "+(28*multiplier)+" kcal (Beeren)\n");
                options.append("   - Makros: P:"+(12*multiplier)+"g / C:"+(8*multiplier)+"g / F:"+(0.5*multiplier)+"g\n");
                options.append("   🔹 Gesamt: "+(70+(28*multiplier))+" kcal\n");
                break;
        }

        return options.toString();
    }

    private void getMahlzeiten(UserData user, String chatId) {
        StringBuilder plan = new StringBuilder();
        double targetCalories = user.dailyCalories;
        String goal = "";

        plan.append("🍽 *Basierend auf deinem Kalorienbedarf kannst du folgende Mahlzeiten essen:*\n\n");

        // Motivations-GIF senden
        SendAnimation animation = new SendAnimation();
        animation.setChatId(chatId);
        animation.setAnimation(new InputFile("https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExODJmbWM2eTR5cWlua2p1MXMxN3I2eDQ0MTlqMHB0MnRhNDE0azYwNiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/5ev3alRsskWA0/giphy.gif"));
        try {
            execute(animation);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        // Abnehmen
        if (user.weight > user.idealWeight) {
            targetCalories -= 500;
            goal = "LOSE";
            plan.append("🔽 *Ziel: Abnehmen*\n");
            plan.append("- Ca. ").append(String.format("%.0f", targetCalories)).append(" kcal pro Tag konsumieren\n");
        }

        // Zunehmen
        else if (user.weight < user.idealWeight) {
            targetCalories += 500;
            goal = "GAIN";
            plan.append("🔼 *Ziel: Zunehmen*\n");
            plan.append("- Ca. ").append(String.format("%.0f", targetCalories)).append(" kcal pro Tag konsumieren\n");
        }

        // Gewicht halten
        else {
            goal = "MAINTAIN";
            plan.append("✅ *Ziel: Gewicht halten*\n");
            plan.append("- Ca. ").append(String.format("%.0f", targetCalories)).append(" kcal pro Tag konsumieren\n");
        }


        plan.append("🍳 *Frühstück (3 Optionen):*\n");
        plan.append(getMealOptions("breakfast", goal));

        plan.append("\n🍎 *Zwischenmahlzeit (3 Optionen):*\n");
        plan.append(getMealOptions("snack", goal));

        plan.append("\n🍲 *Mittagessen (3 Optionen):*\n");
        plan.append(getMealOptions("lunch", goal));

        plan.append("\n🥗 *Abendessen (3 Optionen):*\n");
        plan.append(getMealOptions("dinner", goal));

        SendMessage rezept = new SendMessage();
        rezept.setChatId(chatId);
        rezept.setText(plan.toString());
        rezept.setParseMode("Markdown");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("Zurück zum Hauptmenü")
                        .callbackData("BACK_TO_MAIN")
                        .build()));
        keyboard.setKeyboard(rows);
        rezept.setReplyMarkup(keyboard);
        send(rezept);
    }

    private String getNutritionPlan(UserData user) {
        double tdee = calculateTDEE(user);
        StringBuilder plan = new StringBuilder();

        plan.append("🍎 *Täglicher Ernährungsplan*\n\n");

        if (user.weight > user.idealWeight) {
            double deficit = tdee - 500;
            plan.append("Zum Abnehmen:\n");
            plan.append("- Ca. ").append(String.format("%.0f", deficit)).append(" kcal pro Tag konsumieren\n");
            plan.append("- Fokus auf Proteine: ").append(String.format("%.0f", user.weight * 1.8)).append(" g/Tag\n");
            plan.append("- Vermeide einfache Kohlenhydrate und Zucker\n");
        } else if (user.weight < user.idealWeight) {
            double surplus = tdee + 500;
            plan.append("Zum Zunehmen:\n");
            plan.append("- Ca. ").append(String.format("%.0f", surplus)).append(" kcal pro Tag konsumieren\n");
            plan.append("- Erhöhe gesunde Kohlenhydrate und gute Fette\n");
        } else {
            plan.append("Zum Gewichtserhalt:\n");
            plan.append("- Ca. ").append(String.format("%.0f", tdee)).append(" kcal pro Tag konsumieren\n");
            plan.append("- Achte auf ein ausgewogenes Nährstoffverhältnis\n");
        }
        return plan.toString();
    }

    private String getCompleteHealthReport(UserData user) {
        StringBuilder report = new StringBuilder();

        report.append("📊 *Vollständiger Gesundheitsbericht*\n\n");
        report.append("• *Alter:* ").append(user.age).append(" Jahre\n");
        report.append("• *Geschlecht:* ").append(user.gender.equals("Männlich") ? "Männlich" : "Weiblich").append("\n");
        report.append("• *Größe:* ").append(user.height).append(" cm\n");
        report.append("• *Gewicht:* ").append(user.weight).append(" kg\n");
        report.append("• *BMI (Body-Mass-Index):* ").append(String.format("%.1f", user.bmi)).append("\n");
        report.append("• *Idealgewicht:* ").append(String.format("%.1f", user.idealWeight)).append(" kg\n");
        report.append("• *Tägliche Kalorien zur Gewichtserhaltung:* ").append(String.format("%.0f", user.dailyCalories)).append(" kcal\n\n");

        report.append(getNutritionPlan(user)); // du kannst auch eine deutsche Version davon aufrufen, z.B. getNutritionPlanDE(user)

        return report.toString();
    }

    // handel fun und calc
    private void handleTimeSelection(String chatId, String callbackData, UserData user) {
        // time workout 15,30,60!
        user.time = callbackData.replace("TIME_", "");
        DBHelper.saveUserData(chatId, user);

        // for show list of workout
        try {
            Thread.sleep(1000);
            sendExerciseMenu(chatId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleActivityLevel(String chatId){

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🏋️ *Wie oft trainierst du pro Woche?*\n\n"
                + "1. ⏳ Wenig aktiv (1–2 Mal)\n"
                + "2. 🚶‍♂️ Mäßig aktiv (3–4 Mal)\n"
                + "3. 🏃‍♂️ Aktiv (5–7 Mal)\n");
        message.setParseMode("Markdown");

        //add button für choice
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Arrays.asList(
                InlineKeyboardButton.builder().text("1–2 Mal").callbackData("ACT_LOW").build(),
                InlineKeyboardButton.builder().text("3–4 Mal").callbackData("ACT_MED").build(),
                InlineKeyboardButton.builder().text("5–7 Mal").callbackData("ACT_HIGH").build()
        ));
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        send(message);
    }

    private void handleActivitySelection(String chatId, String activity, UserData user) {
        user.activityLevel = activity;
        // wie oft tag pro woche
        switch(activity) {
            case "ACT_LOW":
                user.workoutsPerWeek = 2;
                break;

            case "ACT_MED":
                user.workoutsPerWeek = 4;
                break;
            case "ACT_HIGH":
                user.workoutsPerWeek = 6;
                break;
        }

        //   calc weight und calories
        user.bmi = calculateBMI(user.height, user.weight);
        user.idealWeight = calculateIdealWeight(user);
        user.dailyCalories = calculateTDEE(user);

        DBHelper.saveUserData(chatId, user);

        //send report
        SendMessage report = new SendMessage();
        report.setChatId(chatId);
        report.setText(getCompleteHealthReport(user));
        report.setParseMode("Markdown");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Arrays.asList(
                InlineKeyboardButton.builder()
                        .text("🍉🥚Rezept")
                        .callbackData("Rezept")
                        .build(),
                InlineKeyboardButton.builder()
                        .text("📊 Show Progress Gewicht")
                        .callbackData("SHOW_PROGRESS")
                        .build()
        ));

        rows.add(Arrays.asList(
                InlineKeyboardButton.builder()
                        .text("🔔 Erinnerungen Wasser aktivieren")
                        .callbackData("REMINDER_ON")
                        .build(),
                InlineKeyboardButton.builder()
                        .text("🔕 Erinnerungen Wasser deaktivieren")
                        .callbackData("REMINDER_OFF")
                        .build()
        ));


        keyboard.setKeyboard(rows);
        report.setReplyMarkup(keyboard);

        send(report);

        // for show list of time workout
        try {
            Thread.sleep(1000);
            listTimeWorkout(chatId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //workout 5-Funktion
    private void sendExerciseMenu(String chatId) {
        try {
            // Motivations-GIF senden
            SendAnimation animation = new SendAnimation();
            SendAnimation animation2 = new SendAnimation();
            animation.setChatId(chatId);
            animation2.setChatId(chatId);
            animation.setAnimation(new InputFile("https://media0.giphy.com/media/v1.Y2lkPTc5MGI3NjExaXB6dHdja3dwNmJsZzJhZmJlaDY2NGdpanlvMmcxaWVsanJ1czQ5OCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/oShObTfbg3S5G/giphy.gif"));
            animation2.setAnimation(new InputFile("https://media0.giphy.com/media/v1.Y2lkPTc5MGI3NjExcWdudXlzeTA3bjBxYnp5aW1kN25jdGllemxucDBud21nbjVvMWFlMCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/lOqNS2HUyN8OJV0CuF/giphy.gif"));
            execute(animation);
            execute(animation2);

            // Trainingsmenü senden
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("🏋️‍♂️ *Wählen Sie Ihr Training:*");
            message.setParseMode("Markdown");

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            rows.add(Collections.singletonList(
                    InlineKeyboardButton.builder()
                            .text("Cardio 🤾‍♂️")
                            .callbackData("CARDIO")
                            .build()));

            rows.add(Collections.singletonList(
                    InlineKeyboardButton.builder()
                            .text("Kraft 💪")
                            .callbackData("STRENGTH")
                            .build()));

            rows.add(Collections.singletonList(
                    InlineKeyboardButton.builder()
                            .text("Flexibilität 💃🧘")
                            .callbackData("FLEXIBILITY")
                            .build()));

            rows.add(Collections.singletonList(
                    InlineKeyboardButton.builder()
                            .text("Zurück zum Hauptmenü")
                            .callbackData("BACK_TO_MAIN")
                            .build()));

            keyboard.setKeyboard(rows);
            message.setReplyMarkup(keyboard);

            send(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void listTimeWorkout(String chatId){

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🏋️ *Wie viel Zeit pro Tag?*\n\n"
                + "1. ⏳ 15 Minuten\n"
                + "2. 🚶‍♂️ 30 Minuten \n"
                + "3. 🏃‍♂️ 60 Minuten\n");
        message.setParseMode("Markdown");

        //add button für choice
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Arrays.asList(
                InlineKeyboardButton.builder().text("15 Minuten").callbackData("TIME_15").build(),
                InlineKeyboardButton.builder().text("30 Minuten").callbackData("TIME_30").build(),
                InlineKeyboardButton.builder().text("60 Minuten").callbackData("TIME_60").build()
        ));
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        send(message);
    }

    private void sendFLEXIBILITYWorkout(String chatId, UserData user) {
        StringBuilder workout = new StringBuilder();
        workout.append("🧘‍♀️ Flexibilitäts übungen für ").append(user.time).append(" Minuten: \n\n");

        switch (user.time) {
            case "15":
                workout.append("1. Dehnung der Waden (2 Minuten)\n")
                        .append("2. Oberschenkeldehnung (2 Minuten)\n")
                        .append("3. Rückenstrecker (2 Minuten)\n")
                        .append("4. Schulterdehnung (2 Minuten)\n")
                        .append("5. Yoga-Übung: Katze-Kuh (3 Minuten)\n")
                        .append("6. Hüftöffner (2 Minuten)\n")
                        .append("7. Nackenentspannung (2 Minuten)\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=i6TzP2COtow");
                break;
            case "30":
                workout.append("1. Dynamisches Aufwärmen (5 Minuten)\n")
                        .append("2. Yoga-Flow (15 Minuten):\n")
                        .append("   - Sonnengruß\n   - Dreieck-Pose\n   - Herabschauender Hund\n")
                        .append("3. Tiefe Dehnungen (10 Minuten):\n")
                        .append("   - Vorbeuge\n   - Taube-Pose\n   - Schmetterling\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=Wa_FD6EsBSg");
                break;
            case "60":
                workout.append("1. Komplettes Yoga-Programm (60 Minuten):\n")
                        .append("   - Aufwärmen (10 Minuten)\n")
                        .append("   - Stehende Posen (15 Minuten)\n")
                        .append("   - Bodenübungen (20 Minuten)\n")
                        .append("   - Tiefenentspannung (15 Minuten)\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=to_USs3Ip6I");
                break;
            default:
                workout.append("Basis-Flexibilitätsübungen:\n")
                        .append("- Schulterkreisen\n")
                        .append("- Rumpfbeugen\n")
                        .append("- Beindehnungen\n");
        }
        workout.append("\n💡 Tipps:\n- Atmen Sie gleichmäßig\n- Gehen Sie nur so weit, wie es sich gut anfühlt\n- Halten Sie jede Dehnung 15-30 Sekunden");
        sendWorkoutMessage(chatId, workout.toString());
    }

    private void sendSTRENGTHWorkout(String chatId, UserData user) {
        StringBuilder workout = new StringBuilder();
        workout.append("💪Kraft übungen für ").append(user.time).append(" Minuten: \n\n");

        switch (user.time) {
            case "15":
                workout.append("1. Schnelles Warm-up (2 Minuten)\n")
                        .append("2. Kniebeugen (3 Sätze à 12 Wiederholungen)\n")
                        .append("3. Liegestütze (3 Sätze à 10 Wiederholungen)\n")
                        .append("4. Ausfallschritte (2 Sätze à 10 pro Bein)\n")
                        .append("5. Plank (3 x 30 Sekunden)\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=lvU8Mhsi7rw");
                break;
            case "30":
                workout.append("1. Aufwärmen (5 Minuten)\n")
                        .append("2. Zirkeltraining (25 Minuten):\n")
                        .append("   - Kniebeugen (45 Sek.)\n")
                        .append("   - Liegestütze (45 Sek.)\n")
                        .append("   - Klimmzüge (45 Sek.)\n")
                        .append("   - Plank (45 Sek.)\n")
                        .append("   - Ausfallschritte (45 Sek.)\n")
                        .append("   - Pause (1 Minute)\n")
                        .append("   (3 Runden)\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=tj0o8aH9vJw");
                break;
            case "60":
                workout.append("1. Komplettes Krafttraining (60 Minuten):\n")
                        .append("   - Aufwärmen (10 Minuten)\n")
                        .append("   - Unterkörper (20 Minuten)\n")
                        .append("   - Oberkörper (20 Minuten)\n")
                        .append("   - Core (10 Minuten)\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=rWearms3rFY");
                break;
            default:
                workout.append("Basis-Kraftübungen:\n")
                        .append("- Kniebeugen\n")
                        .append("- Liegestütze\n")
                        .append("- Plank\n");
        }

        workout.append("\n💡 Tipps:\n- Auf richtige Form achten\n- Langsam steigern\n- 30-60 Sekunden Pause zwischen Sätzen");

        sendWorkoutMessage(chatId, workout.toString());
    }

    private void sendCardioWorkout(String chatId, UserData user) {
        StringBuilder workout = new StringBuilder();
        workout.append("🏃‍♂️Cardio übungen für ").append(user.time).append(" Minuten: \n\n");

        switch (user.time) {
            case "15":
                workout.append("1. Laufen auf der Stelle (3 Minuten)\n")
                        .append("2. Seilspringen (3 Minuten)\n")
                        .append("3. Burpees (3 Sätze)\n")
                        .append("4. Mountain Climbers (3 Minuten)\n")
                        .append("5. Hampelmänner (3 Minuten)\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=VWj8ZxCxrYk");

                break;
            case "30":
                workout.append("1. Aufwärmen: Zügiges Gehen (5 Minuten)\n")
                        .append("2. Intervalllauf (10 Minuten)\n")
                        .append("3. Zirkeltraining (15 Minuten):\n")
                        .append("   - Seilspringen 3 Minuten\n")
                        .append("   - Burpees 3 Minuten\n")
                        .append("   - Hampelmänner 3 Minuten\n")
                        .append("   - Mountain Climbers 3 Minuten\n")
                        .append("   - Pause 3 Minuten\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=ml6cT4AZdqI");
                break;
            case "60":
                workout.append("1. Aufwärmen: Gehen/Joggen (10 Minuten)\n")
                        .append("2. Intervalllauf (20 Minuten):\n")
                        .append("   - 1 Minute schnelles Laufen\n")
                        .append("   - 1 Minute Gehen\n")
                        .append("3. HIIT-Training (20 Minuten):\n")
                        .append("   - 30 Sekunden Arbeit\n")
                        .append("   - 30 Sekunden Pause\n")
                        .append("4. Abkühlen und Dehnen (10 Minuten)\n")
                        .append("Video Z.B : https://www.youtube.com/watch?v=yrNU9Q1XHYw");
                break;
            default:
                workout.append("Allgemeines Cardio-Training:\n")
                        .append("- Schnelles Laufen\n")
                        .append("- Seilspringen\n")
                        .append("- Treppensteigen\n")
                        .append("- Radfahren\n");
        }

        workout.append("\n💡 Tipps:\n- Bleiben Sie hydriert\n- Passen Sie die Intensität an\n- Hören Sie auf, wenn Sie Schmerzen spüren");

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(workout.toString());
        message.setParseMode("Markdown");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(Collections.singletonList(
                Collections.singletonList(
                        InlineKeyboardButton.builder()
                                .text("Zurück zum Hauptmenü")
                                .callbackData("BACK_TO_MAIN")
                                .build()
                )
        ));

        message.setReplyMarkup(keyboard);
        send(message);
    }

    // Water Reminder jeder stunde
    private void handleReminderSubscription(String chatId, UserData user, boolean subscribe) {
        user.subscribedToReminders = subscribe;
        DBHelper.saveUserData(chatId, user);
        SendMessage reply = new SendMessage();
        reply.setChatId(chatId);
        if (subscribe) {
            reply.setText("✅ Erinnerungen Wasser💧aktiviert");
            // Erinnerungen planen
            scheduler.scheduleAtFixedRate(() -> sendWaterReminder(chatId), 9, 2, TimeUnit.HOURS);
        }
        else {
            reply.setText("❌ Erinnerungen Wasser💧deaktiviert");
        }
        reply.setParseMode("Markdown");
        send(reply);
    }

    private void sendWaterReminder(String chatId) {
        SendMessage reminder = new SendMessage();
        reminder.setChatId(chatId);
        reminder.setText("⏰Vergessen Sie nicht, Wasser zu trinken 🚰 💧");
        send(reminder);
    }

    //  ShowProgress Gewicht
    private void showProgressChart(String chatId) {
        Map<String, Double> progress = DBHelper.loadProgress(chatId);
        UserData user = DBHelper.loadUserData(chatId);

        if (progress.isEmpty()) {
            send(new SendMessage(chatId, "📊 Es wurden noch keine Fortschritte aufgezeichnet."));
            return;
        }

        StringBuilder chart = new StringBuilder("📈 Dein Gewichtsverlauf:\n\n");

        assert user != null;
        progress.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    chart.append(entry.getKey()).append(": ").append(entry.getValue()).append(" kg");

                    double diff = entry.getValue() - user.idealWeight;
                    if (diff > 0) {
                        chart.append(" (+" + String.format("%.1f", diff) + " über Idealgewicht)");
                    } else if (diff < 0) {
                        chart.append(" (" + String.format("%.1f", -diff) + " unter Idealgewicht)");
                    } else {
                        chart.append(" (Idealgewicht erreicht)");
                    }

                    chart.append("\n");
                });

        send(new SendMessage(chatId, chart.toString()));
    }

    // für neue Gewicht(history)
    private void handleWeightInput(String chatId, String message, UserData user) {
        try {
            //.split(" ")[2]
            double newWeight = Double.parseDouble(message);
            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            // حفظ التقدم في الـ db
            DBHelper.saveProgress(chatId, currentDate, newWeight);

            // تحديث الوزن الحالي
            user.weight = newWeight;
            DBHelper.saveUserData(chatId, user);

            SendMessage reply = new SendMessage();
            reply.setChatId(chatId);
            reply.setText("✅ *Neues Gewicht gespeichert:* " + newWeight + " kg\n");
            reply.setParseMode("Markdown");
            send(reply);

        } catch (Exception e) {
            SendMessage reply = new SendMessage();
            reply.setChatId(chatId);
            reply.setText("❗ Bitte verwenden Sie das Format: Gewicht speichern [Gewicht]\nBeispiel: Gewicht speichern 68.5");
            send(reply);
        }
    }


    @Override
    public String getBotUsername() {
        return "gesunderfreund_bot"; // username
    }

    @Override
    public String getBotToken() {
        return "7636548833:AAFT7TI7XtPWGAdrNi0YtGvFhcQZ_InuN5s"; //Token
    }

}

