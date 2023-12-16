import pandas as pd
import sklearn
# Sample DataFrame with 'daily_return' column
data = {'daily_return': [0.02, -0.03, 0.01, -0.02, 0.03, -0.01, -0.02, 0.01, 0.02, -0.01]}
df = pd.DataFrame(data)

# Calculate the maximum drawdown using rolling
cumsum = df['daily_return'].cumsum()

max_drawdown = min(df['daily_return'].cumsum().iloc[::-1].expanding().apply(lambda x: min(x) - x.iloc[-1]))
print(max_drawdown)

peak = mdd = 0
for _, item in df['daily_return'].cumsum().items():
    if item > peak:
        peak = item
        mdd = 0
    else:
        mdd = min(item - peak, mdd)
print(mdd)