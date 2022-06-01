# Blackjack trainer
Options
* Minimum wager.
* Credit limit.
* [Multiple (2, 4, 6, 8) | Single] deck(s).
* Dealer [stands | hits] on soft 17.
* Double on any 2 cards. Double [allowed | not allowed] after split.
* Surrender [allowed | not allowed].
* Sudden death mode (requires 100% accuracy to continue play).
* Sounds.
* Display win-loss stats.
* Display doubled win-loss stats.
* Display blackjack stats.

The above options are implemented in config.json as in this example:
```
{
  "min_wager": 5,
  "credit_limit": 1000,
  "num_decks": 8,
  "dealer_stands_soft17": true,
  "double_allowed_after_split": true,
  "surrender_allowed": false,
  "complete_accuracy": true,
  "sounds": true,
  "display_winloss_stat": true,
  "display_doubled_winloss_stat": true,
  "display_blackjack_stat": true
}
```
