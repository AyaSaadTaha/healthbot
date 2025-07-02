package org.example;

import java.util.HashMap;
import java.util.Map;

public class UserData {
    int step = 0;
    String gender;
    int age;
    double weight;
    double height;
    double bmi;
    boolean subscribedToReminders = false;
    Map<String, Double> progress = new HashMap<>();
    String time = "30";

    int workoutsPerWeek;
    String activityLevel;
    double goalWeight;
    double idealWeight;
    double dailyCalories;
}
