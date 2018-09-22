package xyz.elmot.clion.cubemx;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import xyz.elmot.clion.openocd.OpenOcdSettingsState;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static xyz.elmot.clion.openocd.OpenOcdComponent.require;

public class SelectBoardDialog extends DialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(DialogWrapper.class);
    private final String[] values;
    private int result = 0;

    private SelectBoardDialog(Project project, String[] values) {
        super(project, false, false);
        this.values = values;
        setTitle("Board Config Files");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        //todo normal texts
        JBList<String> boardList = new JBList<>(values);
        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardList.setSelectedIndex(result);
        boardList.addListSelectionListener(e -> result = boardList.getSelectedIndex());
        boardList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!e.isPopupTrigger() && e.getClickCount() > 1) {
                    clickDefaultButton();
                }
            }
        });
        new ListSpeedSearch<>(boardList);
        return new JBScrollPane(boardList);
    }

    public static String selectBoardByPriority(ProjectData projectData, Project project) {
        try {
            List<Pair<String, Integer>> keywordWeight = new ArrayList<>();
            String board = Objects.toString(projectData.getBoard(), "").toUpperCase();
            if (board.isEmpty()) {
                LOGGER.info("Board is not defined in the project");
                return null;
            }
            keywordWeight.add(Pair.pair(board, 1000));
            Stream.of("NUCLEO", "EVAL").filter(board::contains).findFirst().ifPresent(s -> keywordWeight.add(Pair.pair(s, 100)));
            if (board.contains("DISCOVERY")) {
                keywordWeight.add(Pair.pair("DISCOVERY", 100));
                keywordWeight.add(Pair.pair("DISC", 20));
            } else if (board.contains("DISC")) {
                keywordWeight.add(Pair.pair("DISC", 100));
                keywordWeight.add(Pair.pair("DISCOVERY", 20));
            }
            String mcuFamily = Objects.toString(projectData.getMcuFamily(), "").toUpperCase();
            for (int i = mcuFamily.length() - 1; i >= 6; i--) {
                keywordWeight.add(Pair.pair(mcuFamily.substring(0, i), i));
            }
            OpenOcdSettingsState ocdSettings = project.getComponent(OpenOcdSettingsState.class);
            VirtualFile ocdHome = require(LocalFileSystem.getInstance().findFileByPath(ocdSettings.openOcdHome));
            VirtualFile ocdScripts = require(OpenOcdSettingsState.findOcdScripts(ocdHome));
            VirtualFile[] ocdBoards = require(ocdScripts.findChild("board")).getChildren();
            Stream<String> boardByScores =
                    Stream.of(ocdBoards).parallel().map(ocdBoard ->
                            {
                                try {
                                    String text = VfsUtil.loadText(ocdBoard).toUpperCase();
                                    int weight = keywordWeight.parallelStream()
                                            .filter(entry -> text.contains(entry.getFirst()))
                                            .mapToInt(pair -> pair.getSecond()).sum();
                                    return Pair.pair(ocdBoard.getName(), weight);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return Pair.pair(ocdBoard.getName(), 0);
                                }
                            }

                    )
                            // Show all
                            //.filter(p -> p.getSecond() != 0)
                            .sorted(Comparator.comparingInt(p -> -p.getSecond()))
                            .map(p -> p.getFirst());
            String[] values = boardByScores.toArray(String[]::new);
            SelectBoardDialog dialog = new SelectBoardDialog(project, values);
            dialog.show();
            int i = dialog.getExitCode();
            if (i == OK_EXIT_CODE) {
                return "board/" + dialog.values[dialog.result];
            }
        } catch (Throwable e) {
            LOGGER.error(e);
        }
        return null;
    }

}
