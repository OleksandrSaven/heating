package ua.home.heatingautomation2026.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import ua.home.heatingautomation2026.dto.DashboardResponse;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.service.DashboardService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Огляд | Home Heating")
public class DashboardView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Kyiv"));

    private final DashboardService dashboardService;

    private final Span totalDevicesValue = new Span("—");
    private final Span activeDevicesValue = new Span("—");
    private final Span readingsValue = new Span("—");

    private final Grid<TelemetryReadingResponse> readingsGrid =
            new Grid<>(TelemetryReadingResponse.class, false);

    private Registration pollRegistration;

    public DashboardView(DashboardService dashboardService) {
        this.dashboardService = dashboardService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);
        addClassName("page-view");

        H1 title = new H1("Система опалення");

        Button refreshButton = new Button(
                "Оновити",
                event -> refreshData()
        );

        HorizontalLayout header =
                new HorizontalLayout(title, refreshButton);
        header.addClassName("page-header");

        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.expand(title);

        Div statistics = new Div(
                createStatCard("Всього пристроїв", totalDevicesValue),
                createStatCard("Активні пристрої", activeDevicesValue),
                createStatCard("Останні показники", readingsValue)
        );

        statistics.addClassName("stats-grid");

/*        statistics.getStyle()
                .set("display", "grid")
                .set(
                        "grid-template-columns",
                        "repeat(auto-fit, minmax(220px, 1fr))"
                )
                .set("gap", "16px")
                .set("width", "100%");*/

        H2 readingsTitle = new H2("Останні показники");

        Paragraph description = new Paragraph(
                "Останні отримані значення від зареєстрованих пристроїв."
        );

        configureReadingsGrid();

        add(
                header,
                statistics,
                readingsTitle,
                description,
                readingsGrid
        );

        refreshData();
    }

    private Div createStatCard(String title, Span value) {

        H3 heading = new H3(title);

        value.getStyle()
                .set("font-size", "2rem")
                .set("font-weight", "600");

        Div card = new Div(heading, value);
        card.addClassName("stat-card");

/*
        card.getStyle()
                .set("padding", "20px")
                .set("border-radius", "12px")
                .set("background", "var(--lumo-base-color)")
                .set(
                        "border",
                        "1px solid var(--lumo-contrast-10pct)"
                )
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("box-sizing", "border-box");
*/

        return card;
    }

    private void configureReadingsGrid() {
        readingsGrid.addClassName("responsive-grid");
        readingsGrid.addColumn(
                        TelemetryReadingResponse::deviceName
                )
                .setHeader("Пристрій")
                .setAutoWidth(true)
                .setFlexGrow(1);

        readingsGrid.addColumn(
                        TelemetryReadingResponse::parameterName
                )
                .setHeader("Параметр")
                .setAutoWidth(true)
                .setFlexGrow(1);

        readingsGrid.addColumn(
                        TelemetryReadingResponse::parameterCode
                )
                .setHeader("Код параметра")
                .setAutoWidth(true)
                .setFlexGrow(1);

        readingsGrid.addColumn(this::formatValue)
                .setHeader("Значення")
                .setAutoWidth(true);

        readingsGrid.addColumn(
                        reading -> reading.unit() == null
                                ? "—"
                                : reading.unit()
                )
                .setHeader("Одиниця")
                .setAutoWidth(true);

        readingsGrid.addColumn(
                        reading -> formatTimestamp(
                                reading.measuredAt() != null
                                        ? reading.measuredAt()
                                        : reading.receivedAt()
                        )
                )
                .setHeader("Час вимірювання")
                .setAutoWidth(true);

        readingsGrid.setWidthFull();
        readingsGrid.setHeight("400px");
    }

    private void refreshData() {

        DashboardResponse data =
                dashboardService.getDashboard();

        totalDevicesValue.setText(
                String.valueOf(data.totalDevices())
        );

        activeDevicesValue.setText(
                String.valueOf(data.activeDevices())
        );

        readingsValue.setText(
                String.valueOf(data.latestReadings().size())
        );

        readingsGrid.setItems(data.latestReadings());
    }

    private String formatValue(
            TelemetryReadingResponse reading
    ) {
        if (reading.numericValue() != null) {
            return reading.numericValue()
                    .stripTrailingZeros()
                    .toPlainString();
        }

        if (reading.booleanValue() != null) {
            return reading.booleanValue() ? "Так" : "Ні";
        }

        if (reading.textValue() != null) {
            return reading.textValue();
        }

        return "—";
    }

    private String formatTimestamp(Instant timestamp) {

        if (timestamp == null) {
            return "—";
        }

        return DATE_FORMAT.format(timestamp);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();

        // Оновлення кожні 5 секунд
        ui.setPollInterval(5_000);

        // Отримання актуальних даних
        pollRegistration = ui.addPollListener(
                event -> refreshData()
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {

        // Видаляємо слухача, щоб не було зайвих запитів
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }

        // Вимикаємо polling, коли залишаємо цю сторінку
        detachEvent.getUI().setPollInterval(-1);

        super.onDetach(detachEvent);
    }
}