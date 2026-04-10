package com.example.luma.models;

import java.util.ArrayList;
import java.util.List;

public class CrisisTime {

    public static class CrisisOption {
        private String id;
        private String label;
        private String emoji;
        private String category;
        private boolean selected;

        public CrisisOption(String id, String emoji, String label, String category) {
            this.id = id;
            this.emoji = emoji;
            this.label = label;
            this.category = category;
            this.selected = false;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getEmoji() { return emoji; }
        public String getCategory() { return category; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
        public void toggle() { this.selected = !this.selected; }
    }

    public static List<CrisisOption> getDefaultOptions() {
        List<CrisisOption> options = new ArrayList<>();

        options.add(new CrisisOption("breathing",   "🌬", "נשימות מודרכות",             "הרגעה מיידית"));
        options.add(new CrisisOption("music",        "🎵", "מוזיקה מרגיעה",              "הרגעה מיידית"));
        options.add(new CrisisOption("grounding",    "✋", "טכניקת 5-4-3-2-1",           "הרגעה מיידית"));
        options.add(new CrisisOption("timer",        "⏱", "טיימר הרגעה (5 דקות)",       "הרגעה מיידית"));
        options.add(new CrisisOption("affirmations", "💬", "אמירות עצמיות חיוביות",      "תמיכה רגשית"));
        options.add(new CrisisOption("reminder",     "🕊", "תזכורת — זה יעבור",          "תמיכה רגשית"));
        options.add(new CrisisOption("journal",      "📓", "כתיבה חופשית / יומן",        "תמיכה רגשית"));
        options.add(new CrisisOption("talk",         "🤝", "שיחה עם אדם קרוב",           "תמיכה רגשית"));
        options.add(new CrisisOption("video",        "📱", "סרטון מצחיק / מרגיע",        "הסחת דעת"));
        options.add(new CrisisOption("walk",         "🚶", "הליכה קצרה בחוץ",            "הסחת דעת"));
        options.add(new CrisisOption("creative",     "🎨", "פעילות יצירתית",             "הסחת דעת"));
        options.add(new CrisisOption("shower",       "🛁", "מקלחת / אמבטיה חמה",         "הסחת דעת"));
        options.add(new CrisisOption("hotline",      "📞", "קו חירום נפשי",              "עזרה מקצועית"));
        options.add(new CrisisOption("therapist",    "🩺", "פנייה למטפל / פסיכולוג",     "עזרה מקצועית"));

        return options;
    }

    public static List<CrisisOption> getSelectedOptions(List<CrisisOption> options) {
        List<CrisisOption> selected = new ArrayList<>();
        for (CrisisOption opt : options) {
            if (opt.isSelected()) selected.add(opt);
        }
        return selected;
    }
}