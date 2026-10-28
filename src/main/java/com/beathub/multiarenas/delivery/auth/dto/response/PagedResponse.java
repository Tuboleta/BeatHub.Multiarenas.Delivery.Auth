package com.beathub.multiarenas.delivery.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envoltorio para respuestas con paginación")
public class PagedResponse<T> {

    @Schema(description = "Lista de elementos en la página actual")
    private List<T> content;

    @Schema(description = "Número de página actual (0-indexed)", example = "0")
    private int pageNumber;

    @Schema(description = "Tamaño de página solicitado", example = "20")
    private int pageSize;

    @Schema(description = "Total de elementos disponibles", example = "42")
    private long totalElements;

    @Schema(description = "Total de páginas calculadas", example = "5")
    private int totalPages;

    @Schema(description = "Indica si es la última página", example = "false")
    private boolean last;

    @Schema(description = "Indica si es la primera página", example = "true")
    private boolean first;

    public static <T> PagedResponse<T> from(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}

