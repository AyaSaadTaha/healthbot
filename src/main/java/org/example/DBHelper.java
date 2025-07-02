package org.example;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.Map;

public class DBHelper {

    private static final String MONGO_URI = "mongodb+srv://ayasaadtaha:pdkcMEi7G5cE33P3@telegramhealthbot.hdkqmdy.mongodb.net/?retryWrites=true&w=majority&appName=TelegramHealthBot";
    private static MongoClient mongoClient;
    private static MongoDatabase database;


    static {
        initializeDatabase();
    }

    /**
     * Datenbankverbindung initialisieren
     */
    public static void initializeDatabase() {
        try {
            // 1.Erstellen Sie eine Verbindung mit MongoDB Atlas
            mongoClient = MongoClients.create(MONGO_URI);

            // 2. Wählen Sie die Datenbank aus (sie wird automatisch erstellt,
            // wenn sie nicht existiert)
            database = mongoClient.getDatabase("healthbot");

            System.out.println("Erfolgreiche Verbindung mit MongoDB!");
        } catch (Exception e) {
            System.err.println("Datenbank Verbindungsfehler!!");
            e.printStackTrace();
        }
    }

    /**
     * Benutzerdaten speichern
     */
    public static void saveUserData(String chatId, UserData user) {
        try {
            // 1. Holen Sie sich die Benutzergruppe (sie wird automatisch erstellt, wenn sie nicht existiert)
            MongoCollection<Document> usersCollection = database.getCollection("users");

            // 2. Erstellen Sie ein neues Dokument mit Benutzerdaten
            Document userDoc = new Document()
                    .append("chat_id", chatId)
                    .append("step", user.step)
                    .append("gender", user.gender)
                    .append("age", user.age)
                    .append("weight", user.weight)
                    .append("height", user.height)
                    .append("bmi", user.bmi)
                    .append("subscribed_to_reminders", user.subscribedToReminders)
                    .append("workouts_per_week", user.workoutsPerWeek)
                    .append("activity_level", user.activityLevel)
                    .append("goal_weight", user.goalWeight)
                    .append("ideal_weight", user.idealWeight)
                    .append("daily_calories", user.dailyCalories)
                    .append("time", user.time);

            // 3. Ersetzen Sie das Dokument, wenn es vorhanden ist, oder fügen Sie es hinzu, wenn es neu ist.
            Bson filter = Filters.eq("chat_id", chatId);
            usersCollection.replaceOne(filter, userDoc, new ReplaceOptions().upsert(true));

        } catch (Exception e) {
            System.err.println("Fehler beim Speichern der Benutzerdaten:");
            e.printStackTrace();
        }
    }

    /**
     * Benutzerdaten abrufen
     */
    public static UserData loadUserData(String chatId) {
        try {
            MongoCollection<Document> usersCollection = database.getCollection("users");

            // Einen Benutzer anhand der Chat-ID finden
            Document userDoc = usersCollection.find(Filters.eq("chat_id", chatId)).first();

            if (userDoc == null) {
                return null;
            }

            // Konvertieren Sie das Dokument in ein UserData-Objekt
            UserData user = new UserData();
            user.step = userDoc.getInteger("step");
            user.gender = userDoc.getString("gender");
            user.age = userDoc.getInteger("age");
            user.weight = userDoc.getDouble("weight");
            user.height = userDoc.getDouble("height");
            user.bmi = userDoc.getDouble("bmi");
            user.subscribedToReminders = userDoc.getBoolean("subscribed_to_reminders");
            user.workoutsPerWeek = userDoc.getInteger("workouts_per_week");
            user.activityLevel = userDoc.getString("activity_level");
            user.goalWeight = userDoc.getDouble("goal_weight");
            user.idealWeight = userDoc.getDouble("ideal_weight");
            user.dailyCalories = userDoc.getDouble("daily_calories");
            user.time = userDoc.getString("time");

            return user;

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Benutzerdaten:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Benutzerfortschritt speichern
     */
    public static void saveProgress(String chatId, String date, double weight) {
        try {
            MongoCollection<Document> progressCollection = database.getCollection("progress");

            Document progressDoc = new Document()
                    .append("chat_id", chatId)
                    .append("date", date)
                    .append("weight", weight);

            Bson filter = Filters.and(
                    Filters.eq("chat_id", chatId),
                    Filters.eq("date", date)
            );

            progressCollection.replaceOne(filter, progressDoc, new ReplaceOptions().upsert(true));

        } catch (Exception e) {
            System.err.println("Fehler beim Speichern des Fortschritts:");
            e.printStackTrace();
        }
    }

    /**
     * Benutzerfortschritt abrufen
     */
    public static Map<String, Double> loadProgress(String chatId) {
        Map<String, Double> progressMap = new HashMap<>();

        try {
            MongoCollection<Document> progressCollection = database.getCollection("progress");

            // Alle Fortschrittsaufzeichnungen nach Datum sortiert abrufen
            FindIterable<Document> progressDocs = progressCollection
                    .find(Filters.eq("chat_id", chatId))
                    .sort(Sorts.ascending("date"));

            for (Document doc : progressDocs) {
                progressMap.put(doc.getString("date"), doc.getDouble("weight"));
            }

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen des Fortschritts:");
            e.printStackTrace();
        }

        return progressMap;
    }

    /**
     * Schließen Sie die Verbindung, wenn Sie fertig sind.
     */
    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
