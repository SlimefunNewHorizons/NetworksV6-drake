package io.github.sefiraat.networks.slimefun.network.grid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GridCache {

    private int page;
    private int maxPages;
    @Nonnull
    private SortOrder sortOrder;
    @Nullable
    private String filter;

    public GridCache(int page, int maxPages, @Nonnull SortOrder sortOrder) {
        this.page = page;
        this.maxPages = maxPages;
        this.sortOrder = sortOrder;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    /**
     * Pagina siguiente acotada al maximo publicado. El maximo se sanea aqui tambien porque una
     * rejilla vacia llego a publicar -1 y el boton dejaba la cache en la pagina -1, lo que hacia
     * que {@code AbstractGrid#updateDisplay} calculara un indice de subList negativo.
     */
    public static int nextPage(int page, int maxPages) {
        final int limit = Math.max(0, maxPages);
        return page >= limit ? limit : page + 1;
    }

    /** Pagina anterior, nunca por debajo de la pagina 0. */
    public static int previousPage(int page) {
        return page <= 0 ? 0 : page - 1;
    }

    @Nonnull
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(@Nonnull SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Nullable
    public String getFilter() {
        return filter;
    }

    public void setFilter(@Nullable String filter) {
        this.filter = filter;
    }

    enum SortOrder {
        ALPHABETICAL,
        NUMBER
    }
}
