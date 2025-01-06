package views;

import controllers.DatabaseHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class InventarisApp extends JFrame {
    private JPanel mainPanel;
    private JTextField fieldNama;
    private JComboBox<String> comboKategori;
    private JTextField fieldJumlah;
    private JComboBox<String> comboLokasi;
    private JButton btnTambah;
    private JTable tabelAset;
    private DefaultTableModel model;

    public InventarisApp() {
        initComponents();
        setContentPane(mainPanel);
        setTitle("Aplikasi Inventaris Aset");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Setup tabel
        String[] kolom = {"ID", "Nama", "Kategori", "Jumlah", "Lokasi", "Edit", "Hapus"};
        model = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5 || column == 6; // Hanya kolom Edit dan Hapus yang bisa diklik
            }
        };
        tabelAset.setModel(model);

        // Tambahkan tombol Edit dan Hapus di kolom tabel
        tabelAset.getColumn("Edit").setCellRenderer(new ButtonRenderer("Edit"));
        tabelAset.getColumn("Hapus").setCellRenderer(new ButtonRenderer("Hapus"));
        tabelAset.getColumn("Edit").setCellEditor(new ButtonEditor(new JCheckBox(), "Edit"));
        tabelAset.getColumn("Hapus").setCellEditor(new ButtonEditor(new JCheckBox(), "Hapus"));

        loadKategori();
        loadLokasi();
        loadData();

        btnTambah.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tambahAset();
            }
        });
    }

    private void loadKategori() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nama_kategori FROM kategori_aset")) {

            comboKategori.removeAllItems();
            while (rs.next()) {
                comboKategori.addItem(rs.getString("nama_kategori"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data kategori!");
        }
    }

    private void loadLokasi() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nama_lokasi FROM lokasi")) {

            comboLokasi.removeAllItems();
            while (rs.next()) {
                comboLokasi.addItem(rs.getString("nama_lokasi"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data lokasi!");
        }
    }

    private void loadData() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM aset")) {

            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("nama_aset"),
                        rs.getString("kategori"),
                        rs.getInt("jumlah"),
                        rs.getString("lokasi"),
                        "Edit",
                        "Hapus"
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data aset!");
        }
    }

    private void tambahAset() {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO aset (nama_aset, kategori, jumlah, lokasi) VALUES (?, ?, ?, ?)")) {

            stmt.setString(1, fieldNama.getText());
            stmt.setString(2, (String) comboKategori.getSelectedItem());
            stmt.setInt(3, Integer.parseInt(fieldJumlah.getText()));
            stmt.setString(4, (String) comboLokasi.getSelectedItem());

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data berhasil ditambahkan!");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menambahkan data!");
        }
    }

    private void editAset(int id) {
    try (Connection conn = DatabaseHelper.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM aset WHERE id = ?")) {

        stmt.setInt(1, id);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            // Data lama
            String namaLama = rs.getString("nama_aset");
            String kategoriLama = rs.getString("kategori");
            int jumlahLama = rs.getInt("jumlah");
            String lokasiLama = rs.getString("lokasi");

            // Form dialog untuk edit data
            JTextField fieldEditNama = new JTextField(namaLama, 20);
            JComboBox<String> comboEditKategori = new JComboBox<>();
            JComboBox<String> comboEditLokasi = new JComboBox<>();
            JTextField fieldEditJumlah = new JTextField(String.valueOf(jumlahLama), 10);

            // Load data kategori dan lokasi ke combo box
            loadComboBox(comboEditKategori, "SELECT nama_kategori FROM kategori_aset", kategoriLama);
            loadComboBox(comboEditLokasi, "SELECT nama_lokasi FROM lokasi", lokasiLama);

            // Panel untuk form
            JPanel panelEdit = new JPanel();
            panelEdit.setLayout(new BoxLayout(panelEdit, BoxLayout.Y_AXIS));
            panelEdit.add(new JLabel("Nama Aset:"));
            panelEdit.add(fieldEditNama);
            panelEdit.add(new JLabel("Kategori:"));
            panelEdit.add(comboEditKategori);
            panelEdit.add(new JLabel("Jumlah:"));
            panelEdit.add(fieldEditJumlah);
            panelEdit.add(new JLabel("Lokasi:"));
            panelEdit.add(comboEditLokasi);

            // Tampilkan dialog
            int result = JOptionPane.showConfirmDialog(this, panelEdit, "Edit Data Aset",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                // Ambil data baru dari form
                String namaBaru = fieldEditNama.getText();
                String kategoriBaru = (String) comboEditKategori.getSelectedItem();
                int jumlahBaru = Integer.parseInt(fieldEditJumlah.getText());
                String lokasiBaru = (String) comboEditLokasi.getSelectedItem();

                // Update ke database
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE aset SET nama_aset = ?, kategori = ?, jumlah = ?, lokasi = ? WHERE id = ?")) {

                    updateStmt.setString(1, namaBaru);
                    updateStmt.setString(2, kategoriBaru);
                    updateStmt.setInt(3, jumlahBaru);
                    updateStmt.setString(4, lokasiBaru);
                    updateStmt.setInt(5, id);

                    updateStmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Data berhasil diperbarui!");
                    loadData();
                }
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Gagal memperbarui data!");
    }
}

    private void loadComboBox(JComboBox<String> comboBox, String query, String selectedItem) {
    comboBox.removeAllItems();
    try (Connection conn = DatabaseHelper.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

        while (rs.next()) {
            String item = rs.getString(1);
            comboBox.addItem(item);
        }

        // Pilih item sesuai data lama
        comboBox.setSelectedItem(selectedItem);
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Gagal memuat data ke combo box!");
    }
}


    private void hapusAset(int id) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM aset WHERE id = ?")) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data berhasil dihapus!");
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menghapus data!");
        }
    }

    private void initComponents() {
        mainPanel = new JPanel();
        fieldNama = new JTextField(20);
        comboKategori = new JComboBox<>();
        fieldJumlah = new JTextField(10);
        comboLokasi = new JComboBox<>();
        btnTambah = new JButton("Tambah");
        JButton btnLaporanKategori = new JButton("Laporan Kategori");
        JButton btnLaporanLokasi = new JButton("Laporan Lokasi");
        tabelAset = new JTable();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(new JLabel("Nama Aset:"));
        mainPanel.add(fieldNama);

        mainPanel.add(new JLabel("Kategori:"));
        mainPanel.add(comboKategori);

        mainPanel.add(new JLabel("Jumlah:"));
        mainPanel.add(fieldJumlah);

        mainPanel.add(new JLabel("Lokasi:"));
        mainPanel.add(comboLokasi);

        mainPanel.add(btnTambah);
        mainPanel.add(new JScrollPane(tabelAset));

        // Tambahkan tombol laporan
        mainPanel.add(btnLaporanKategori);
        mainPanel.add(btnLaporanLokasi);

        // Event listener untuk laporan
        btnLaporanKategori.addActionListener(e -> tampilkanLaporanKategori());
        btnLaporanLokasi.addActionListener(e -> tampilkanLaporanLokasi());
    }
    
    private void tampilkanLaporanKategori() {
        String query = "SELECT kategori, COUNT(*) AS jumlah_aset, SUM(jumlah) AS total_jumlah "
                     + "FROM aset GROUP BY kategori";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Model tabel untuk laporan
            DefaultTableModel laporanModel = new DefaultTableModel(new String[]{"Kategori", "Jumlah Aset", "Total Jumlah"}, 0);

            while (rs.next()) {
                laporanModel.addRow(new Object[]{
                        rs.getString("kategori"),
                        rs.getInt("jumlah_aset"),
                        rs.getInt("total_jumlah")
                });
            }

            // Tampilkan laporan di dialog
            JTable laporanTable = new JTable(laporanModel);
            JOptionPane.showMessageDialog(this, new JScrollPane(laporanTable), "Laporan Aset Per Kategori", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat laporan kategori!");
        }
    }
    
    private void tampilkanLaporanLokasi() {
        String query = "SELECT lokasi, COUNT(*) AS jumlah_aset, SUM(jumlah) AS total_jumlah "
                     + "FROM aset GROUP BY lokasi";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Model tabel untuk laporan
            DefaultTableModel laporanModel = new DefaultTableModel(new String[]{"Lokasi", "Jumlah Aset", "Total Jumlah"}, 0);

            while (rs.next()) {
                laporanModel.addRow(new Object[]{
                        rs.getString("lokasi"),
                        rs.getInt("jumlah_aset"),
                        rs.getInt("total_jumlah")
                });
            }

            // Tampilkan laporan di dialog
            JTable laporanTable = new JTable(laporanModel);
            JOptionPane.showMessageDialog(this, new JScrollPane(laporanTable), "Laporan Aset Per Lokasi", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat laporan lokasi!");
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventarisApp().setVisible(true));
    }

    // Renderer untuk tombol
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(String label) {
            setText(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    // Editor untuk tombol
    class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private int id;

        public ButtonEditor(JCheckBox checkBox, String label) {
            super(checkBox);
            button = new JButton(label);
            button.addActionListener(e -> {
                int row = tabelAset.getSelectedRow();
                id = (int) tabelAset.getValueAt(row, 0);

                if (label.equals("Edit")) {
                    editAset(id);
                } else if (label.equals("Hapus")) {
                    hapusAset(id);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (String) value;
            button.setText(label);
            return button;
        }
    }
}