package com.balugaq.netex.utils;

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro por nombre de los buscadores (grids, managers y visor de flujo).
 *
 * Antes cada buscador, por cada item y en cada actualizacion, evaluaba filter.matches("^[a-zA-Z]+$")
 * --que compila la regex en cada llamada-- y convertia el nombre a pinyin dos veces. En un grid
 * abierto eso es por tick y por item de la red.
 *
 * El resultado es el mismo: el nombre coincide si lo contiene, y si el filtro es solo letras ASCII,
 * tambien si lo contiene su pinyin (completo o de iniciales). El pinyin de cada nombre se guarda.
 * No se puede saltar para nombres sin caracteres chinos: la libreria tambien quita los espacios
 * ("iron sword" da "ironsword"), y eso deja que "ironsword" encuentre el item.
 */
@UtilityClass
public class NameFilter {
    private static final int MAX_CACHED_NAMES = 20_000;
    private static final Map<String, String[]> PINYIN_CACHE = new ConcurrentHashMap<>();

    public static boolean matches(@NotNull String name, @NotNull String filter) {
        if (name.contains(filter)) {
            return true;
        }
        if (!isLettersOnly(filter)) {
            return false;
        }
        final String[] pinyin = pinyinOf(name);
        return pinyin[0].contains(filter) || pinyin[1].contains(filter);
    }

    /**
     * Lo mismo que filter.matches("^[a-zA-Z]+$"), sin regex.
     */
    public static boolean isLettersOnly(@NotNull String filter) {
        if (filter.isEmpty()) {
            return false;
        }
        for (int i = 0; i < filter.length(); i++) {
            final char c = filter.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    private static String @NotNull [] pinyinOf(@NotNull String name) {
        String[] cached = PINYIN_CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        // Tope de memoria: los nombres son finitos, pero los renombrados con yunque no tienen limite.
        if (PINYIN_CACHE.size() >= MAX_CACHED_NAMES) {
            PINYIN_CACHE.clear();
        }
        cached = new String[]{
            PinyinHelper.toPinyin(name, PinyinStyleEnum.INPUT, ""),
            PinyinHelper.toPinyin(name, PinyinStyleEnum.FIRST_LETTER, "")
        };
        PINYIN_CACHE.put(name, cached);
        return cached;
    }
}
