"""Charts for the knapsack benchmark.

Reads the two result files written by knapsack.bench.Benchmark and writes the
figures used in the README.

    python3 scripts/plot.py
"""

from pathlib import Path

import matplotlib
matplotlib.use("Agg")

import matplotlib.pyplot as plt
import pandas as pd

ROOT = Path(__file__).resolve().parent.parent
RESULTS = ROOT / "results"

SURFACE = "#fcfcfb"
INK = "#0b0b0b"
INK_SOFT = "#52514e"
GRID = "#e4e3df"

# One fixed colour per solver, assigned once and never reused for anything else,
# so a solver keeps its colour across every figure.
COLOUR = {
    "brute-force": "#2a78d6",
    "branch-and-bound": "#eb6834",
    "dp": "#1baf7a",
    "greedy": "#eda100",
}

SOLVER_ORDER = ["brute-force", "branch-and-bound", "dp", "greedy"]

CLASS_LABEL = {
    "UNCORRELATED": "uncorrelated",
    "WEAKLY_CORRELATED": "weakly corr.",
    "STRONGLY_CORRELATED": "strongly corr.",
    "INVERSE_STRONGLY_CORRELATED": "inverse strongly corr.",
    "ALMOST_STRONGLY_CORRELATED": "almost strongly corr.",
    "SUBSET_SUM": "subset sum",
}


def load():
    frames = []
    for name in ("small", "large"):
        frame = pd.read_csv(RESULTS / f"{name}.csv")
        frame["set"] = name
        frames.append(frame)
    return pd.concat(frames, ignore_index=True)


def style(ax, title, xlabel, ylabel):
    ax.set_title(title, color=INK, fontsize=13, pad=14, loc="left")
    ax.set_xlabel(xlabel, color=INK_SOFT, fontsize=10)
    ax.set_ylabel(ylabel, color=INK_SOFT, fontsize=10)
    ax.grid(True, color=GRID, linewidth=0.8, zorder=0)
    ax.set_axisbelow(True)
    for side in ("top", "right"):
        ax.spines[side].set_visible(False)
    for side in ("left", "bottom"):
        ax.spines[side].set_color(GRID)
    ax.tick_params(colors=INK_SOFT, labelsize=9)


def save(fig, name):
    path = RESULTS / name
    fig.savefig(path, dpi=160, bbox_inches="tight", facecolor=SURFACE)
    plt.close(fig)
    print(f"wrote {path.relative_to(ROOT)}")


def chart_time(data):
    """Running time against instance size, both axes logarithmic."""
    ok = data[data.status == "ok"]
    fig, ax = plt.subplots(figsize=(8, 4.8), facecolor=SURFACE)
    ax.set_facecolor(SURFACE)

    for solver in SOLVER_ORDER:
        series = (ok[ok.solver == solver]
                  .groupby("n")["time_ms"].median()
                  .sort_index())
        if series.empty:
            continue
        ax.plot(series.index, series.values,
                color=COLOUR[solver], linewidth=2, marker="o", markersize=5,
                markeredgecolor=SURFACE, markeredgewidth=1, label=solver, zorder=3)
        # direct label at the end of each line, so identity never rests on colour alone
        ax.annotate(solver, (series.index[-1], series.values[-1]),
                    textcoords="offset points", xytext=(8, 0),
                    color=INK_SOFT, fontsize=9, va="center")

    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlim(right=ax.get_xlim()[1] * 1.6)
    style(ax, "Running time against instance size",
          "items (n), log scale", "median time per solve (ms), log scale")
    ax.legend(frameon=False, labelcolor=INK_SOFT, fontsize=9, loc="upper left")
    save(fig, "time_vs_n.png")


def chart_pruning(data):
    """How much of the search tree the bound removes."""
    small = data[(data.set == "small") & (data.status == "ok")]
    fig, ax = plt.subplots(figsize=(8, 4.8), facecolor=SURFACE)
    ax.set_facecolor(SURFACE)

    for solver in ("brute-force", "branch-and-bound"):
        series = (small[small.solver == solver]
                  .groupby("n")["nodes"].median()
                  .sort_index())
        ax.plot(series.index, series.values,
                color=COLOUR[solver], linewidth=2, marker="o", markersize=5,
                markeredgecolor=SURFACE, markeredgewidth=1, label=solver, zorder=3)
        ax.annotate(solver, (series.index[-1], series.values[-1]),
                    textcoords="offset points", xytext=(8, 0),
                    color=INK_SOFT, fontsize=9, va="center")

    ax.set_yscale("log")
    ax.set_xlim(right=ax.get_xlim()[1] + 4)
    style(ax, "Search nodes visited, with and without the bound",
          "items (n)", "median nodes visited, log scale")
    ax.legend(frameon=False, labelcolor=INK_SOFT, fontsize=9, loc="upper left")
    save(fig, "nodes_pruning.png")


def greedy_error(data):
    """Greedy's shortfall against the optimum, where an exact solver finished."""
    ok = data[data.status == "ok"]
    exact = ok[ok.solver.isin(["brute-force", "dp", "branch-and-bound"])]
    optimum = exact.groupby(["set", "id"])["value"].max().rename("optimum")

    greedy = ok[ok.solver == "greedy"].merge(optimum, on=["set", "id"])
    greedy["error"] = (greedy.optimum - greedy.value) / greedy.optimum * 100
    return greedy


def chart_error_by_class(greedy):
    by_class = (greedy.groupby("type")["error"].mean()
                .sort_values(ascending=True))

    fig, ax = plt.subplots(figsize=(8, 4.2), facecolor=SURFACE)
    ax.set_facecolor(SURFACE)

    labels = [CLASS_LABEL[t] for t in by_class.index]
    bars = ax.barh(labels, by_class.values,
                   color=COLOUR["greedy"], height=0.62, zorder=3)

    for bar, value in zip(bars, by_class.values):
        ax.annotate(f"{value:.2f}%", (bar.get_width(), bar.get_y() + bar.get_height() / 2),
                    textcoords="offset points", xytext=(6, 0),
                    color=INK_SOFT, fontsize=9, va="center")

    ax.set_xlim(right=by_class.max() * 1.22)
    style(ax, "How far greedy falls short, by instance class",
          "mean shortfall against the optimum (%)", "")
    save(fig, "greedy_error_by_class.png")


def chart_error_by_size(greedy):
    by_size = greedy.groupby("n")["error"].mean().sort_index()

    fig, ax = plt.subplots(figsize=(8, 4.2), facecolor=SURFACE)
    ax.set_facecolor(SURFACE)

    ax.plot(by_size.index, by_size.values,
            color=COLOUR["greedy"], linewidth=2, marker="o", markersize=5,
            markeredgecolor=SURFACE, markeredgewidth=1, zorder=3)

    ax.set_xscale("log")
    style(ax, "Greedy gets more accurate as instances grow",
          "items (n), log scale", "mean shortfall against the optimum (%)")
    save(fig, "greedy_error_vs_n.png")


def main():
    data = load()
    chart_time(data)
    chart_pruning(data)

    greedy = greedy_error(data)
    chart_error_by_class(greedy)
    chart_error_by_size(greedy)

    print()
    print("mean shortfall by class (%):")
    print(greedy.groupby("type")["error"].agg(["mean", "max"]).round(2).to_string())


if __name__ == "__main__":
    main()
