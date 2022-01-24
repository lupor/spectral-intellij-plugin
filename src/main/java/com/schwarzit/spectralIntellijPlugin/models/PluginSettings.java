package com.schwarzit.spectralIntellijPlugin.models;

import com.schwarzit.spectralIntellijPlugin.config.Config;
import com.schwarzit.spectralIntellijPlugin.settings.BaseSettingsComponent;

import java.util.Objects;

public class PluginSettings {
    private String ruleset;
    private String includedFiles;

    @SuppressWarnings("unused") // NoArgs constructor is needed for serialization
    public PluginSettings() {
    }

    public PluginSettings(String ruleset, String includedFiles) {
        this.ruleset = ruleset;
        this.includedFiles = includedFiles;
    }

    public String getRuleset() {
        return ruleset;
    }

    public void setRuleset(String ruleset) {
        if (ruleset.isBlank()) {
            this.ruleset = Config.Instance.DEFAULT_RULESET_URL();
        } else {
            this.ruleset = ruleset;
        }
    }

    public String getIncludedFiles() {
        return includedFiles;
    }

    public void setIncludedFiles(String includedFiles) {
        if (includedFiles.isBlank()) {
            this.includedFiles = Config.Instance.DEFAULT_INCLUDED_FILES_PATTERN();
        } else {
            this.includedFiles = includedFiles;
        }
    }

    public boolean equals(BaseSettingsComponent ui) {
        return Objects.equals(this.ruleset, ui.getRulesetPath()) && Objects.equals(this.includedFiles, ui.getIncludedFiles());
    }
}