# Blackjack trainer
Options
1. [Multiple (2, 4, 6, 8) | Single] deck(s).
2. Dealer [stands | hits] on soft 17.
3. Double on any 2 cards. Double [allowed | not allowed] after split.
4. Surrender [allowed | not allowed].

The above options can be implemented in config.json as in this example:
```
{
  "running_total": 20.0,
  "num_decks": 4,
  "dealer_stands_soft17": true,
  "double_allowed_after_split": true,
  "surrender_allowed": false
}
```
