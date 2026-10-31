package smartstay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import smartstay.model.BookingStatus;
import smartstay.model.Guest;
import smartstay.model.Invoice;
import smartstay.model.Reservation;
import smartstay.model.Room;
import smartstay.model.RoomType;
import smartstay.security.AccessControl;
import smartstay.security.AccessDeniedException;
import smartstay.service.InvoiceService;
import smartstay.service.ReservationService;
import smartstay.util.FormatUtil;
import smartstay.util.InvoiceCalculator;

/**
 * Detailed invoice screen with View and Print.
 * The booking list only contains bookings the current user may see, and
 * InvoiceService checks access again, so a guest can only open their own
 * invoices.
 */
public class InvoiceFrame extends BaseFrame {

    private static final long serialVersionUID = 1L;

    private final InvoiceService invoiceService = new InvoiceService();
    private final ReservationService reservationService = new ReservationService();

    private final JComboBox<String> bookingCombo = UITheme.createComboBox(new String[0]);
    private final InvoicePaper paper = new InvoicePaper();
    private Invoice currentInvoice;

    /** @param bookingId booking to show straight away, or null to let the user choose */
    public InvoiceFrame(String bookingId) {
        super("Invoices", "Detailed bills with discounts, tax and the booking advance.");
        AccessControl.requireLogin();

        AppButton backButton = new AppButton("Back", AppButton.Style.SECONDARY);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(new ReservationManagementFrame(false));
            }
        });
        AppButton dashboardButton = new AppButton("Dashboard", AppButton.Style.SECONDARY);
        dashboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToDashboard();
            }
        });
        addPageAction(backButton);
        addPageAction(dashboardButton);

        getContentArea().add(createToolbar(), BorderLayout.NORTH);
        JPanel paperHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        paperHolder.setBackground(UITheme.BACKGROUND);
        paperHolder.add(paper);
        JScrollPane scroll = new JScrollPane(paperHolder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(UITheme.BACKGROUND);
        getContentArea().add(scroll, BorderLayout.CENTER);

        loadBookingList(bookingId);
        viewInvoice();
    }

    private JPanel createToolbar() {
        CardPanel bar = new CardPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        bookingCombo.setPreferredSize(new Dimension(420, UITheme.FIELD_HEIGHT));
        AppButton viewButton = new AppButton("View Invoice", AppButton.Style.PRIMARY);
        AppButton printButton = new AppButton("Print Invoice", AppButton.Style.SECONDARY);
        bar.add(UITheme.createFieldLabel("Booking"));
        bar.add(bookingCombo);
        bar.add(viewButton);
        bar.add(printButton);

        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewInvoice();
            }
        });
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printInvoice();
            }
        });
        return bar;
    }

    /** Only bookings the user is allowed to see are listed. */
    private void loadBookingList(String selectBookingId) {
        List<Reservation> reservations = reservationService.getVisibleReservations();
        for (Reservation r : reservations) {
            String item = r.getBookingId() + "  •  Room " + r.getRoomId() + "  •  "
                    + FormatUtil.formatDate(r.getCheckInDate()) + "  •  " + r.getStatus();
            bookingCombo.addItem(item);
            if (r.getBookingId().equals(selectBookingId)) {
                bookingCombo.setSelectedItem(item);
            }
        }
    }

    private void viewInvoice() {
        Object selected = bookingCombo.getSelectedItem();
        if (selected == null) {
            paper.showEmpty();
            return;
        }
        String bookingId = selected.toString().split(" ")[0];
        try {
            currentInvoice = invoiceService.getInvoice(bookingId);
            paper.showInvoice(currentInvoice);
        } catch (AccessDeniedException | IllegalArgumentException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    /** Prints the invoice using the standard Java printing API. */
    private void printInvoice() {
        if (currentInvoice == null) {
            UITheme.showError(this, "Please choose a booking and view its invoice first.");
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("SmartStay Invoice " + currentInvoice.getReservation().getBookingId());
        job.setPrintable(paper);
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                UITheme.showError(this, "Printing failed: " + ex.getMessage());
            }
        }
    }

    // =====================================================================
    // The invoice "paper"
    // =====================================================================

    /** White A4-style panel that draws the invoice and can print itself. */
    private static class InvoicePaper extends JPanel implements Printable {

        private static final long serialVersionUID = 1L;
        private static final int WIDTH = 760;

        InvoicePaper() {
            setBackground(Color.WHITE);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        }

        void showEmpty() {
            removeAll();
            JLabel empty = new JLabel("No bookings yet - make a reservation to see an invoice here.");
            empty.setFont(UITheme.FONT_BODY);
            empty.setForeground(UITheme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));
            add(empty);
            setPreferredSize(new Dimension(WIDTH, 180));
            revalidate();
            repaint();
        }

        void showInvoice(Invoice invoice) {
            removeAll();
            Reservation r = invoice.getReservation();
            Guest guest = invoice.getGuest();
            Room room = invoice.getRoom();
            RoomType type = r.getRoomType();

            add(createHeader(invoice));

            // Booking, guest and room details side by side
            JPanel details = new JPanel(new GridLayout(1, 3, 18, 0));
            details.setOpaque(false);
            details.setBorder(BorderFactory.createEmptyBorder(22, 30, 8, 30));
            details.add(createBlock("BOOKING DETAILS", new String[][]{
                {"Booking ID", r.getBookingId()},
                {"Booking Date", FormatUtil.formatDateTime(r.getBookingDateTime())},
                {"Check-in", FormatUtil.formatDate(r.getCheckInDate())},
                {"Check-out", FormatUtil.formatDate(r.getCheckOutDate())},
                {"Status", r.getStatus().getDisplayName()}}));
            details.add(createBlock("GUEST DETAILS", new String[][]{
                {"Name", guest == null ? "-" : guest.getFullName()},
                {"NIC/Passport", guest == null ? "-" : guest.getNicOrPassport()},
                {"Contact", guest == null ? "-" : guest.getContactNumber()},
                {"Email", guest == null ? "-" : guest.getEmail()}}));
            details.add(createBlock("ROOM DETAILS", new String[][]{
                {"Room ID", r.getRoomId()},
                {"Room Type", type.getDisplayName()},
                {"Bed Type", room == null ? "-" : room.getBedType().getDisplayName()},
                {"Daily Rate", FormatUtil.formatMoney(type.getDailyRate())},
                {"Nights", String.valueOf(invoice.getNumberOfNights())},
                {"Guests", String.valueOf(r.getNumberOfGuests())},
                {"Breakfast", type.hasComplimentaryBreakfast() ? "Complimentary" : "Not included"}}));
            add(details);

            add(createBilling(invoice));
            add(createFooter(invoice));

            setPreferredSize(null);
            setPreferredSize(new Dimension(WIDTH, getPreferredSize().height));
            revalidate();
            repaint();
        }

        private JPanel createHeader(Invoice invoice) {
            Reservation r = invoice.getReservation();
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(UITheme.NAVY);
            header.setBorder(BorderFactory.createEmptyBorder(22, 30, 22, 30));

            JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            brand.setOpaque(false);
            brand.add(new LogoMark(46));
            JPanel brandText = new JPanel(new GridLayout(3, 1));
            brandText.setOpaque(false);
            JLabel hotel = new JLabel("SmartStay Hotels");
            hotel.setFont(new Font("Georgia", Font.BOLD, 20));
            hotel.setForeground(Color.WHITE);
            brandText.add(hotel);
            brandText.add(whiteSmall("SmartStay Hotel Booking System"));
            brandText.add(whiteSmall("Sri Lanka"));
            brand.add(brandText);
            header.add(brand, BorderLayout.WEST);

            JPanel title = new JPanel(new GridLayout(3, 1));
            title.setOpaque(false);
            JLabel invoiceLabel = new JLabel("INVOICE", SwingConstants.RIGHT);
            invoiceLabel.setFont(new Font("Georgia", Font.BOLD, 26));
            invoiceLabel.setForeground(UITheme.ACCENT);
            title.add(invoiceLabel);
            JLabel number = whiteSmall("No. INV-" + r.getBookingId());
            number.setHorizontalAlignment(SwingConstants.RIGHT);
            title.add(number);
            JLabel generated = whiteSmall("Generated " + FormatUtil.formatDateTime(invoice.getGeneratedDateTime()));
            generated.setHorizontalAlignment(SwingConstants.RIGHT);
            title.add(generated);
            header.add(title, BorderLayout.EAST);
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            return header;
        }

        private JPanel createBlock(String heading, String[][] rows) {
            JPanel block = new JPanel(new GridBagLayout());
            block.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 8, 0);
            JLabel title = new JLabel(heading);
            title.setFont(UITheme.FONT_LABEL.deriveFont(11f));
            title.setForeground(UITheme.PRIMARY);
            block.add(title, c);
            c.gridwidth = 1;
            for (int i = 0; i < rows.length; i++) {
                c.gridy = i + 1;
                c.gridx = 0;
                c.weightx = 0;
                c.insets = new Insets(2, 0, 2, 10);
                JLabel key = new JLabel(rows[i][0]);
                key.setFont(UITheme.FONT_SMALL);
                key.setForeground(UITheme.MUTED);
                block.add(key, c);
                c.gridx = 1;
                c.weightx = 1;
                c.insets = new Insets(2, 0, 2, 0);
                JLabel value = new JLabel(rows[i][1]);
                value.setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
                value.setForeground(UITheme.TEXT);
                block.add(value, c);
            }
            c.gridy = rows.length + 1;
            c.weighty = 1;
            block.add(new JLabel(" "), c);
            return block;
        }

        /** The billing table, in the same six-step order as InvoiceCalculator. */
        private JPanel createBilling(Invoice invoice) {
            Reservation r = invoice.getReservation();
            RoomType type = r.getRoomType();
            JPanel billing = new JPanel(new GridBagLayout());
            billing.setOpaque(false);
            billing.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

            int row = 0;
            row = addBillingHeading(billing, row);
            row = addBillingRow(billing, row, "Base Room Charge",
                    FormatUtil.formatMoney(type.getDailyRate()) + " x " + invoice.getNumberOfNights() + " night(s)",
                    FormatUtil.formatMoney(invoice.getBaseRoomCharge()), false);
            row = addBillingRow(billing, row, "Extra Guest Charges",
                    invoice.getExtraGuests() + " extra guest(s) x " + FormatUtil.formatMoney(type.getExtraGuestCharge())
                    + " x " + invoice.getNumberOfNights() + " night(s)",
                    FormatUtil.formatMoney(invoice.getExtraGuestCharge()), false);
            row = addBillingRow(billing, row, "Discount",
                    invoice.isDiscountApplied() ? "12% long-stay discount on base room charge"
                    : "Not applicable (under " + InvoiceCalculator.LONG_STAY_MIN_NIGHTS + " nights)",
                    "- " + FormatUtil.formatMoney(invoice.getDiscount()), false);
            row = addBillingRow(billing, row, "Taxable Amount", "Base - discount + extra guests",
                    FormatUtil.formatMoney(invoice.getTaxableAmount()), false);
            row = addBillingRow(billing, row, "Tax", type.getTaxPercentage() + "% " + type.getDisplayName()
                    + " room tax", FormatUtil.formatMoney(invoice.getTax()), false);
            row = addBillingRow(billing, row, "Total Charge", "",
                    FormatUtil.formatMoney(invoice.getTotalCharge()), true);
            row = addBillingRow(billing, row, "Booking Advance Deducted",
                    r.getStatus() == BookingStatus.CANCELLED ? "Booking cancelled - advance refunded"
                    : "Refundable advance paid at booking",
                    "- " + FormatUtil.formatMoney(invoice.getAdvanceDeducted()), false);

            // Highlighted final amount
            JPanel finalRow = new JPanel(new BorderLayout());
            finalRow.setBackground(UITheme.LIGHT_BLUE);
            finalRow.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            JLabel finalCaption = new JLabel("FINAL PAYABLE AMOUNT");
            finalCaption.setFont(UITheme.FONT_HEADING);
            finalCaption.setForeground(UITheme.NAVY);
            JLabel finalValue = new JLabel(FormatUtil.formatMoney(invoice.getFinalPayable()));
            finalValue.setFont(UITheme.FONT_TITLE);
            finalValue.setForeground(UITheme.PRIMARY);
            finalRow.add(finalCaption, BorderLayout.WEST);
            finalRow.add(finalValue, BorderLayout.EAST);
            GridBagConstraints c = fullRow(row++);
            c.insets = new Insets(10, 0, 0, 0);
            billing.add(finalRow, c);

            if (invoice.getRefundDue().signum() > 0) {
                JLabel refund = new JLabel("Refund due to guest: " + FormatUtil.formatMoney(invoice.getRefundDue()));
                refund.setFont(UITheme.FONT_LABEL);
                refund.setForeground(UITheme.SUCCESS);
                c = fullRow(row);
                c.insets = new Insets(8, 0, 0, 0);
                billing.add(refund, c);
            }
            return billing;
        }

        private int addBillingHeading(JPanel panel, int row) {
            GridBagConstraints c = fullRow(row);
            JLabel title = new JLabel("BILLING DETAILS");
            title.setFont(UITheme.FONT_LABEL.deriveFont(11f));
            title.setForeground(UITheme.PRIMARY);
            c.insets = new Insets(0, 0, 6, 0);
            panel.add(title, c);
            c = fullRow(row + 1);
            panel.add(new JSeparator(), c);
            return row + 2;
        }

        private int addBillingRow(JPanel panel, int row, String item, String detail, String amount, boolean bold) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row;
            c.gridx = 0;
            c.weightx = 0.35;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(7, 0, 7, 8);
            JLabel itemLabel = new JLabel(item);
            itemLabel.setFont(bold ? UITheme.FONT_LABEL.deriveFont(13f) : UITheme.FONT_BODY);
            itemLabel.setForeground(UITheme.TEXT);
            panel.add(itemLabel, c);

            c.gridx = 1;
            c.weightx = 0.45;
            JLabel detailLabel = new JLabel(detail);
            detailLabel.setFont(UITheme.FONT_SMALL);
            detailLabel.setForeground(UITheme.MUTED);
            panel.add(detailLabel, c);

            c.gridx = 2;
            c.weightx = 0.2;
            c.anchor = GridBagConstraints.EAST;
            JLabel amountLabel = new JLabel(amount, SwingConstants.RIGHT);
            amountLabel.setFont(bold ? UITheme.FONT_LABEL.deriveFont(13f) : UITheme.FONT_BODY);
            amountLabel.setForeground(UITheme.TEXT);
            panel.add(amountLabel, c);

            GridBagConstraints line = fullRow(row + 1);
            JSeparator separator = new JSeparator();
            separator.setForeground(UITheme.BORDER);
            panel.add(separator, line);
            return row + 2;
        }

        private JPanel createFooter(Invoice invoice) {
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createEmptyBorder(14, 30, 26, 30));
            String note = invoice.getReservation().getStatus() == BookingStatus.CANCELLED
                    ? "This booking was cancelled. Nothing is payable and the booking advance is refundable."
                    : "The final amount is payable at check-out. The booking advance is refundable.";
            JLabel noteLabel = new JLabel("<html>" + note + "<br><b>Thank you for choosing SmartStay.</b></html>");
            noteLabel.setFont(UITheme.FONT_SMALL);
            noteLabel.setForeground(UITheme.MUTED);
            footer.add(noteLabel, BorderLayout.CENTER);
            return footer;
        }

        private GridBagConstraints fullRow(int row) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = row;
            c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            return c;
        }

        private static JLabel whiteSmall(String text) {
            JLabel label = new JLabel(text);
            label.setFont(UITheme.FONT_SMALL);
            label.setForeground(new Color(200, 214, 233));
            return label;
        }

        /** Scales the invoice to fit one printed page. */
        @Override
        public int print(Graphics graphics, PageFormat format, int pageIndex) {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(format.getImageableX(), format.getImageableY());
            double scale = Math.min(format.getImageableWidth() / getWidth(),
                    format.getImageableHeight() / getHeight());
            if (scale < 1) {
                g2.scale(scale, scale);
            }
            printAll(g2);
            return PAGE_EXISTS;
        }
    }
}
