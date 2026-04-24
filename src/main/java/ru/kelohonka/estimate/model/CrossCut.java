package ru.kelohonka.estimate.model;

/**
 * Описывает переруб в срубе.
 * Переруб увеличивает общий объем бревна, который нужен для проекта.
 *
 * @param name название или комментарий к перерубу
 * @param quantity количество одинаковых перерубов
 * @param customLengthMeters пользовательская длина переруба в метрах,
 *                           если null, длина определяется автоматически по диаметру бревна
 */
public record CrossCut(
        String name,
        int quantity,
        Double customLengthMeters
) {
}
