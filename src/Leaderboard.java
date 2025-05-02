import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

// This class serves as the UI of the leaderboard page
public class Leaderboard extends GameLobby {

    // UI components for leaderboard data
    private JTable leaderboardTable;
    private DefaultTableModel tableModel;

    // The constructor follows superclass, then highlight the "Leaderboard" tab
    public Leaderboard(JPoker24Game gamePlayer, Server gameServer) {
        super(gamePlayer, gameServer);
        highlightActiveTab(leaderboardTab);
    }
    
    @Override
    protected void initializeContent() {
        bodyPanel.setLayout(new BorderLayout());
        bodyPanel.setBorder(new EmptyBorder(20,20,20,20));
        
        // Create table model with column names
        String[] columnNames = {"Rank", "Player", "Games won", "Games played", "Avg. winning time"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        leaderboardTable = new JTable(tableModel);
        leaderboardTable.setFillsViewportHeight(true);
        leaderboardTable.setRowHeight(35);
        leaderboardTable.setIntercellSpacing(new Dimension(10, 0));
        leaderboardTable.setShowGrid(true);
        leaderboardTable.setGridColor(new Color(230, 230, 230));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        for (int i = 0; i < leaderboardTable.getColumnCount(); i++) {
            leaderboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        bodyPanel.add(scrollPane, BorderLayout.CENTER);

        // Populate the table by retrieving user data from the database
        // The row is sorted by rank in ascending order
        try {
            String query = "SELECT * FROM user_info ORDER BY `rank` ASC";
            PreparedStatement statement = super.getDBConn().prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int rank = resultSet.getInt("rank");
                String username = resultSet.getString("username");
                int gamesWon = resultSet.getInt("games_won");
                int gamesPlayed = resultSet.getInt("games_played");
                double avgWinTime = resultSet.getDouble("avg_win_time");
                tableModel.addRow(new Object[] {rank, username, gamesWon, gamesPlayed, avgWinTime+"s"});
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user data: " + e.getMessage());
        }
    }

    public void refreshLeaderboard() {
        // Reset and reinitialize the content
        bodyPanel.removeAll();
        initializeContent();
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }
}