package com.myphotoeditinglibrarys;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ghanshyam on 7/26/2017.
 */
public class FontSetting {

    private static final String DEFAULT_FONT_NAME = "Helvetica";

    private final Map<String, Typeface> typefaces;
    private final Map<String, String> fontNameToTypefaceFile;
    private final Resources resources;
    private final List<String> fontNames;

    public FontSetting(Resources resources) {
        this.resources = resources;
        typefaces = new HashMap<>();
        fontNameToTypefaceFile = new HashMap<>();
        fontNameToTypefaceFile.put("Arial", "Arial.ttf");
        fontNameToTypefaceFile.put("Eutemia", "Eutemia.ttf");
        fontNameToTypefaceFile.put("GREENPIL", "GREENPIL.ttf");
        fontNameToTypefaceFile.put("Grinched", "Grinched.ttf");
        fontNameToTypefaceFile.put("Helvetica", "Helvetica.ttf");
        fontNameToTypefaceFile.put("Libertango", "Libertango.ttf");
        fontNameToTypefaceFile.put("Metal Macabre", "MetalMacabre.ttf");
        fontNameToTypefaceFile.put("Parry Hotter", "ParryHotter.ttf");
        fontNameToTypefaceFile.put("SCRIPTIN", "SCRIPTIN.ttf");
        fontNameToTypefaceFile.put("The Godfather v2", "TheGodfather_v2.ttf");
        fontNameToTypefaceFile.put("Aka Dora", "akaDora.ttf");
        fontNameToTypefaceFile.put("Waltograph", "waltograph42.ttf");

        fontNames = new ArrayList<>(fontNameToTypefaceFile.keySet());
    }

    public Typeface getTypeface(@Nullable String typefaceName) {
        if (TextUtils.isEmpty(typefaceName)) {
            return Typeface.DEFAULT;
        } else {
            if (typefaces.get(typefaceName) == null) {
                typefaces.put(typefaceName,
                        Typeface.createFromAsset(resources.getAssets(), "fonts/" + fontNameToTypefaceFile.get(typefaceName)));
            }
            return typefaces.get(typefaceName);
        }
    }

    public List<String> getFontNames() {
        return fontNames;
    }

    public String getDefaultFontName() {
        return DEFAULT_FONT_NAME;
    }
}