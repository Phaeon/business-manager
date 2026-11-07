package com.businessmanager.bootstrap;

import com.businessmanager.dto.appointment.AppointmentDtos.AppointmentRequest;
import com.businessmanager.dto.common.LineItemDtos.LineItemRequest;
import com.businessmanager.dto.customer.CustomerRequest;
import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceRequest;
import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceResponse;
import com.businessmanager.dto.invoice.InvoiceDtos.PaymentRequest;
import com.businessmanager.dto.project.ProjectDtos.CommentRequest;
import com.businessmanager.dto.project.ProjectDtos.ProjectRequest;
import com.businessmanager.dto.project.ProjectDtos.ProjectResponse;
import com.businessmanager.dto.quote.QuoteDtos.QuoteRequest;
import com.businessmanager.dto.quote.QuoteDtos.QuoteResponse;
import com.businessmanager.dto.task.TaskDtos.TaskRequest;
import com.businessmanager.dto.user.UserDtos.UserCreateRequest;
import com.businessmanager.entity.Role;
import com.businessmanager.entity.TaskPriority;
import com.businessmanager.entity.TaskStatus;
import com.businessmanager.entity.User;
import com.businessmanager.entity.ProjectStatus;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.UserRepository;
import com.businessmanager.service.AppointmentService;
import com.businessmanager.service.CustomerService;
import com.businessmanager.service.InvoiceService;
import com.businessmanager.service.ProjectService;
import com.businessmanager.service.QuoteService;
import com.businessmanager.service.TaskService;
import com.businessmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Jeu de données de démonstration (activé avec APP_SEED_DEMO=true) :
 * permet de visualiser tableau de bord, graphiques, kanban et calendrier dès le premier lancement.
 * Comptes créés : lea@demo.com (employé) et marc@demo.com (comptabilité), mot de passe Demo1234!
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CustomerService customerService;
    private final QuoteService quoteService;
    private final InvoiceService invoiceService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AppointmentService appointmentService;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }
        try {
            seed();
            log.info("Données de démonstration créées");
        } catch (RuntimeException e) {
            log.error("Échec de la création des données de démonstration", e);
        }
    }

    private void seed() {
        User employee = createUserIfAbsent("Léa", "Dubois", "lea@demo.com", Role.EMPLOYEE);
        createUserIfAbsent("Marc", "Leroy", "marc@demo.com", Role.ACCOUNTING);

        List<UUID> customers = new ArrayList<>();
        String[][] data = {
                {"Camille Martin", "Atelier Lumière", "camille@atelier-lumiere.fr", "02 41 00 11 22"},
                {"Thomas Bernard", "Bernard & Fils", "thomas@bernard-fils.fr", "02 41 00 33 44"},
                {"Sophie Lambert", "Lambert Conseil", "sophie@lambert-conseil.fr", "06 12 34 56 78"},
                {"Nicolas Petit", "Studio Petit", "nicolas@studio-petit.fr", "06 98 76 54 32"},
                {"Julie Moreau", "Boulangerie Moreau", "julie@boulangerie-moreau.fr", "02 41 55 66 77"},
                {"Antoine Roux", null, "antoine.roux@example.com", null}
        };
        for (String[] c : data) {
            customers.add(customerService.create(
                    new CustomerRequest(c[0], c[1], c[2], c[3], "Angers (49)", null)).id());
        }

        seedQuotes(customers);
        seedInvoices(customers);
        seedProjects(customers, employee);
        seedAppointments(customers);
        invoiceService.markOverdueInvoices();
    }

    private User createUserIfAbsent(String first, String last, String email, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userService.create(new UserCreateRequest(first, last, email, "Demo1234!", role));
        }
        return userRepository.findByEmail(email).orElseThrow();
    }

    private void seedQuotes(List<UUID> customers) {
        quoteService.create(quoteFor(customers.get(2), "Audit stratégique", 1, "1800"));              // brouillon
        QuoteResponse sent = quoteService.create(quoteFor(customers.get(3), "Identité visuelle", 1, "2200"));
        quoteService.send(sent.id());                                                                  // envoyé
        QuoteResponse accepted = quoteService.create(quoteFor(customers.get(4), "Site e-commerce", 1, "4500"));
        quoteService.accept(accepted.id());                                                            // accepté, à facturer
        QuoteResponse rejected = quoteService.create(quoteFor(customers.get(5), "Refonte logo", 1, "900"));
        quoteService.reject(rejected.id());                                                            // refusé

        QuoteResponse converted = quoteService.create(quoteFor(customers.get(0), "Site vitrine", 1, "2400"));
        quoteService.accept(converted.id());
        invoiceService.createFromQuote(converted.id());                                                // devenu facture brouillon
    }

    private void seedInvoices(List<UUID> customers) {
        String[] amounts = {"1800", "950", "2400", "3200", "1250", "2780", "1500", "3900"};
        LocalDate base = LocalDate.now().withDayOfMonth(15);

        for (int i = 0; i < amounts.length; i++) {
            LocalDate issue = base.minusMonths(amounts.length - i);
            InvoiceResponse invoice = invoiceService.create(new InvoiceRequest(
                    customers.get(i % customers.size()), issue, issue.plusDays(30), null,
                    List.of(line("Prestation de conseil — mois " + (i + 1), 1, amounts[i]))));
            invoiceService.send(invoice.id());
            if (i < amounts.length - 1) {
                invoiceService.pay(invoice.id(), new PaymentRequest(
                        invoice.totalTtc(), issue.plusDays(12), "Virement", "VIR-" + (1000 + i)));
            }
        }

        // Facture dont l'échéance est dépassée -> passera "En retard" avec notification
        LocalDate old = LocalDate.now().minusDays(60);
        InvoiceResponse late = invoiceService.create(new InvoiceRequest(
                customers.get(1), old, old.plusDays(30), "Relance à prévoir",
                List.of(line("Maintenance annuelle", 1, "1200"))));
        invoiceService.send(late.id());
    }

    private void seedProjects(List<UUID> customers, User employee) {
        ProjectResponse site = projectService.create(new ProjectRequest(
                "Refonte du site vitrine", "Nouveau site responsive avec CMS", customers.get(0),
                new BigDecimal("2880"), LocalDate.now().minusDays(20), LocalDate.now().plusDays(2),
                ProjectStatus.IN_PROGRESS));
        task(site, "Maquettes graphiques", TaskStatus.DONE, TaskPriority.HIGH, "12", "11", employee);
        task(site, "Intégration des pages", TaskStatus.IN_REVIEW, TaskPriority.HIGH, "20", "18", employee);
        task(site, "Formulaire de contact", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, "6", "2", employee);
        task(site, "Mise en ligne", TaskStatus.TODO, TaskPriority.LOW, "4", "0", null);
        projectService.addComment(site.id(), new CommentRequest("Le client a validé les maquettes."), employee);

        ProjectResponse shop = projectService.create(new ProjectRequest(
                "Boutique en ligne", "E-commerce avec paiement Stripe", customers.get(4),
                new BigDecimal("5400"), LocalDate.now().plusDays(5), LocalDate.now().plusDays(60),
                ProjectStatus.PLANNED));
        task(shop, "Cahier des charges", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "8", "3", employee);
        task(shop, "Catalogue produits", TaskStatus.TODO, TaskPriority.MEDIUM, "16", "0", null);

        projectService.create(new ProjectRequest(
                "Identité visuelle", "Logo, charte et déclinaisons", customers.get(3),
                new BigDecimal("2640"), LocalDate.now().minusDays(90), LocalDate.now().minusDays(15),
                ProjectStatus.COMPLETED));
    }

    private void seedAppointments(List<UUID> customers) {
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atTime(10, 0);
        appointmentService.create(new AppointmentRequest("Point de cadrage — Lambert Conseil",
                tomorrow, tomorrow.plusHours(1), customers.get(2), "Visio", null));
        LocalDateTime nextWeek = LocalDate.now().plusDays(6).atTime(14, 30);
        appointmentService.create(new AppointmentRequest("Recette site vitrine",
                nextWeek, nextWeek.plusHours(2), customers.get(0), "Atelier Lumière, Angers", null));
        LocalDateTime later = LocalDate.now().plusDays(12).atTime(9, 0);
        appointmentService.create(new AppointmentRequest("Démo boutique en ligne",
                later, later.plusHours(1), customers.get(4), null, null));
    }

    private void task(ProjectResponse project, String title, TaskStatus status, TaskPriority priority,
                      String estimate, String spent, User assignee) {
        taskService.create(new TaskRequest(project.id(), title, null, priority, status,
                new BigDecimal(estimate), new BigDecimal(spent), assignee == null ? null : assignee.getId()));
    }

    private static QuoteRequest quoteFor(UUID customerId, String label, int qty, String price) {
        return new QuoteRequest(customerId, null, null, null, List.of(line(label, qty, price)));
    }

    private static LineItemRequest line(String description, int qty, String unitPrice) {
        return new LineItemRequest(description, BigDecimal.valueOf(qty), new BigDecimal(unitPrice),
                new BigDecimal("20"), BigDecimal.ZERO);
    }
}
