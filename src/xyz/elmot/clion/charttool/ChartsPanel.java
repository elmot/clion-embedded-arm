package xyz.elmot.clion.charttool;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.charttool.state.ChartExpr;
import xyz.elmot.clion.charttool.state.ExpressionState;
import xyz.elmot.clion.charttool.ui.Zoomer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChartsPanel extends JFXPanel {
    private boolean initialized = false;
    public static final int MAX_SERIES = 50;

    private LineChart<Number, Number> lineChart;
    private Map<String, ChartExpressionData> seriesByName = new ConcurrentHashMap<>();

    public ChartsPanel() {
        Platform.runLater(() -> {

            Button reset = new Button("Clear");
            reset.setOnAction(e -> clear());
            Button noZoom = new Button("Reset Zoom");
            //defining the axes
            final NumberAxis xAxis = new NumberAxis();
            final NumberAxis yAxis = new NumberAxis();
            //creating the chart
            lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setCreateSymbols(false);

            Zoomer zoomer = new Zoomer(lineChart);
            noZoom.setOnAction(e -> zoomer.resetZoom());
            VBox vBox = new VBox(10, zoomer, new HBox(10,reset,noZoom));
            vBox.setPadding(new Insets(10));
            Scene scene = new Scene(vBox);

            lineChart.setAnimated(false);
            vBox.setFillWidth(true);
            VBox.setVgrow(zoomer, Priority.ALWAYS);
            lineChart.setScaleShape(true);
            setScene(scene);
            invalidate();
            initialized = true;
        });
    }

    public void clear() {
        seriesByName.clear();
        Platform.runLater(lineChart.getData()::clear);
    }

    public void series(ChartExpr chartExpr, List<Number> numbers) {
        ChartExpressionData data = seriesByName
                .computeIfAbsent(chartExpr.getName(), a -> new ChartExpressionData());
        String name;
        if (chartExpr.getState() == ExpressionState.ACCUMULATE) {
            int index = data.currentIndex.getAndUpdate(i -> (i + 1) % MAX_SERIES);
            if (data.data.size() <= index) {
                data.data.add(numbers);
            } else {
                data.data.set(index, numbers);
            }
            name = accChartName(chartExpr, index);
        } else {
            data.data.clear();
            data.currentIndex.set(0);
            data.data.add(numbers);
            name = chartExpr.getName();
        }

        if (initialized) {
            ObservableList<XYChart.Data<Number, Number>> lineData = calcLineData(chartExpr, numbers);
            Platform.runLater(() -> {
                ObservableList<XYChart.Series<Number, Number>> chartData = lineChart.getData();
                Optional<XYChart.Series<Number, Number>> foundSeries = chartData
                        .stream()
                        .filter(series -> name.equals(series.getName()))
                        .findFirst();
                if (foundSeries.isPresent()) {
                    foundSeries.get().setData(lineData);
                } else {
                    chartData.add(new XYChart.Series<>(name, lineData));
                }
            });
        }
    }

    @NotNull
    protected String accChartName(ChartExpr chartExpr, int index) {
        return chartExpr.getName() + " #" + (index + 1);
    }


    @NotNull
    protected ObservableList<XYChart.Data<Number, Number>> calcLineData(ChartExpr chartExpr, List<Number> numbers) {
        return FXCollections
                .observableArrayList(IntStream.range(0, numbers.size()).mapToObj(
                        i -> {
                            double x = chartExpr.getXBase() + chartExpr.getXScale() * i;
                            double y = chartExpr.getYBase() + chartExpr.getYScale() * numbers.get(i).doubleValue();
                            return new XYChart.Data<>((Number) x, (Number) y);
                        }
                ).collect(Collectors.toList()));
    }

    public void refreshData(Collection<ChartExpr> exprs) {
        if (!initialized) {
            return;
        }
        List<XYChart.Series<Number, Number>> chartData = new ArrayList<>();
        for (ChartExpr expr : exprs) {
            String name = expr.getName();
            ChartExpressionData chartExpressionData = seriesByName.get(name);
            if (chartExpressionData == null) {
                continue;
            }
            if (expr.getState() == ExpressionState.ACCUMULATE) {
                for (int i = 0; i < chartExpressionData.data.size(); i++) {
                    @NotNull ObservableList<XYChart.Data<Number, Number>> numberNumberSeries =
                            calcLineData(expr, chartExpressionData.data.get(i));
                    chartData.add(new XYChart.Series<>(accChartName(expr, i), numberNumberSeries));
                }
            } else if (!chartExpressionData.data.isEmpty()) {
                chartData.add(new XYChart.Series<>(name, calcLineData(expr, chartExpressionData.data.get(0))));
            }
        }
        ObservableList<XYChart.Series<Number, Number>> observableChartData = FXCollections
                .observableArrayList(chartData);
        Platform.runLater(() -> lineChart.setData(observableChartData));
    }

    public boolean isSampled(String name) {
        return seriesByName.containsKey(name);
    }

    private static class ChartExpressionData {
        private final List<List<Number>> data = new ArrayList<>(MAX_SERIES);
        private final AtomicInteger currentIndex = new AtomicInteger();
    }
}
