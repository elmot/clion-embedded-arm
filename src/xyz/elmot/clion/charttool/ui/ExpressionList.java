package xyz.elmot.clion.charttool.ui;

import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import org.jdesktop.swingx.JXRadioGroup;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.charttool.ChartToolPersistence;
import xyz.elmot.clion.charttool.state.ChartExpr;
import xyz.elmot.clion.charttool.state.ExpressionState;

import java.awt.*;
import java.util.stream.Stream;

public class ExpressionList extends AddEditRemovePanel<ChartExpr> {

    private final ChartToolPersistence persistence;
    private final Runnable updateRunner;

    public ExpressionList(ChartToolPersistence persistence, Runnable updateRunner) {
        super(createModel(), persistence.getExprs());
        this.persistence = persistence;
        this.updateRunner = updateRunner;
        getTable().getColumnModel().getColumn(1).setPreferredWidth(80);

    }

    private static TableModel<ChartExpr> createModel() {
        return new TableModel<ChartExpr>() {
            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return new String[]{"Name", "State", "Expression"}[columnIndex];
            }

            @Override
            public boolean isEditable(int column) {
                return column == 1;
            }

            @Override
            public Class getColumnClass(int columnIndex) {
                return columnIndex == 1 ? ExpressionState.class : String.class;
            }

            @Override
            public Object getField(ChartExpr chartExpr, int columnIndex) {
                if (columnIndex == 1) {
                    return chartExpr.getState();
                }
                String name = chartExpr.getName();
                String expr = chartExpr.getExpressionTrim();
                return (expr.equals(name)) ? name : (name + " (" + expr + ")");
            }

        };
    }

    @Override
    protected ChartExpr addItem() {
        persistence.registerChange();
        return doEdit(new ChartExpr());
    }

    @Override
    protected boolean removeItem(ChartExpr chartExpr) {
        persistence.registerChange();
        return true;
    }

    @Nullable
    @Override
    protected ChartExpr editItem(ChartExpr chartExpr) {
        return doEdit(chartExpr);

    }

    @Nullable
    private ChartExpr doEdit(ChartExpr chartExpr) {
        JBTextField expressionField = new JBTextField();
//        expressionField.getDocument().addDocumentListener(e->);//todo enable/disable OK
        JBTextField nameField = new JBTextField();
        JBTextField baseXField = new JBTextField();
        JBTextField baseYField = new JBTextField();

        JBTextField scaleXField = new JBTextField();
        JBTextField scaleYField = new JBTextField();

        JXRadioGroup<ExpressionState> stateGroup = new JXRadioGroup<>(ExpressionState.values());
        Stream.of(ExpressionState.values()).forEach(v -> stateGroup.getChildButton(v).setToolTipText(v.hint));
        stateGroup.setSelectedValue(chartExpr.getState());
        JBPanel<JBPanel> dataPanel = new JBPanel<>(new GridLayout(7, 2));
        dataPanel.add(new JBLabel("Name (optional): "));
        dataPanel.add(nameField);
        dataPanel.add(new JBLabel("Expression: "));
        dataPanel.add(expressionField);
        dataPanel.add(new JBLabel("Action: "));
        dataPanel.add(stateGroup);

        dataPanel.add(new JBLabel("Base X: "));
        dataPanel.add(baseXField);

        dataPanel.add(new JBLabel("Scale X: "));
        dataPanel.add(scaleXField);

        dataPanel.add(new JBLabel("Base Y: "));
        dataPanel.add(baseYField);

        dataPanel.add(new JBLabel("Scale Y: "));
        dataPanel.add(scaleYField);


        DialogBuilder dialogBuilder = new DialogBuilder(this)
                .centerPanel(dataPanel)
                .title("Edit expression");
        dialogBuilder.addOkAction();
        dialogBuilder.addCancelAction();

        expressionField.setText(chartExpr.getExpression());
        nameField.setText(chartExpr.getName());
        stateGroup.setSelectedValue(chartExpr.getState());

        baseXField.setText("" + chartExpr.getXBase());
        baseYField.setText("" + chartExpr.getYBase());
        scaleXField.setText("" + chartExpr.getXScale());
        scaleYField.setText("" + chartExpr.getYScale());

        if (dialogBuilder.showAndGet()) {
            chartExpr.setExpression(expressionField.getText().trim());
            chartExpr.setName(nameField.getText().trim());
            chartExpr.setState(stateGroup.getSelectedValue());

            chartExpr.setXBase(Double.parseDouble(baseXField.getText()));
            chartExpr.setYBase(Double.parseDouble(baseYField.getText()));

            chartExpr.setXScale(Double.parseDouble(scaleXField.getText()));
            chartExpr.setYScale(Double.parseDouble(scaleYField.getText()));

            persistence.registerChange();
            updateRunner.run();
            return chartExpr;
        }
        return null;
    }
}
