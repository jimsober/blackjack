# Blackjack trainer
Options
1. Minimum wager.
2. Credit limit.
3. [Multiple (2, 4, 6, 8) | Single] deck(s).
4. Dealer [stands | hits] on soft 17.
5. Double on any 2 cards. Double [allowed | not allowed] after split.
6. Surrender [allowed | not allowed].
7. Require 100% accuracy to continue play.
8. Sounds.

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
  "sounds": true
}
```
