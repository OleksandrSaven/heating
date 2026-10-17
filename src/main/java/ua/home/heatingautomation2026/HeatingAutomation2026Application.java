package ua.home.heatingautomation2026;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
public class HeatingAutomation2026Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(HeatingAutomation2026Application.class, args);
    }
}
