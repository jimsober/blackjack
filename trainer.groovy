#!/usr/bin/env groovy

/* Blackjack trainer
   =================
   Options
   1. [Multiple (2, 4, 6, 8) | Single] deck(s).
   2. Dealer [stands | hits] on soft 17.
   3. Double on any 2 cards. Double [allowed | not allowed] after split.
   4. Surrender [allowed | not allowed]. */

def init_game() {
    default_style = "${(char)27}[37;40m"
    num_decks = 4
    dealer_stands_soft17 = true
    double_allowed_after_split = true
    surrender_allowed = false
    running_total = 20.0
    total_attempts = 0
    accurate_attempts = 0
    System.out.print("\033[H\033[2J")
    System.out.flush()
    println 'You have been awarded ' + running_total.intValue() + ' free chips!!! Good luck!' + '\7'
    System.console().readLine 'Press <Enter> to continue: '
    System.out.print("\033[H\033[2J")
    System.out.flush()
    return [default_style, running_total, num_decks, dealer_stands_soft17, double_allowed_after_split, surrender_allowed, total_attempts, accurate_attempts]
}

def init_shoe(num_decks) {
    shoe = []
    suits = ['H','C','D','S']
    ranks = ['2','3','4','5','6','7','8','9','0','J','Q','K','A']
    vals = [2,3,4,5,6,7,8,9,10,10,10,10,11] * (4 * num_decks)
    for (i = 0; i < num_decks; i++) {
        for (suit in suits) {
            for (rank in ranks) {
                shoe.add([rank + suit,0])
            }
        for (j = 0; j < shoe.size(); j++) {
            shoe[j][1] = vals[j]
            }
        }
    }
    Collections.shuffle(shoe)
    num_cards = shoe.size()
    cut_index = Math.abs( new Random().nextInt() % (num_cards - (num_cards * .75).intValue()) ) + (num_cards * .75).intValue()
    shoe.addAll(cut_index,[['CC',0]])
    cut_card_drawn = false
    println 'The shoe has been shuffled.' + '\7'
    return [shoe, cut_card_drawn]
}

def init_wager(running_total, total_attempts, accurate_attempts) {
    System.out.flush()
    printf 'House Rules: '
    printf num_decks + ' deck'
    if (num_decks > 1) {
        printf 's'
    }
    printf '. '
    printf 'Dealer '
    if (!dealer_stands_soft17) {
        printf 'hits'
    } else {
        printf 'stands'
    }
    printf ' on soft 17. '
    printf 'Double on any 2 cards. Double '
    if (!double_allowed_after_split) {
        printf 'not '
    }
    printf 'allowed after split. '
    printf 'Surrender '
    if (!surrender_allowed) {
        printf 'not '
    }
    printf 'allowed.\n'
    println()
//    if (total_attempts != 0) {
//        println 'Accuracy: ' + (100 * (accurate_attempts / total_attempts)).round(1).toString() + '%'
//    }
    cash = running_total - running_total.intValue()
    if (cash > 0) {
        println 'Balance: ' + running_total.intValue().toString() + ' chips and $' + cash.toString()
    } else {
        println 'Balance: ' + running_total.intValue().toString()
    }
    try {
        wager
    } catch (MissingPropertyException) {
        if (running_total >= 5) {
            wager = 5
        } else {
            wager = running_total.intValue()
        }
    }
    if (wager > running_total) {
        wager = running_total.intValue()
    }

    input_err = true
    while (input_err) {
        input = System.console().readLine "Enter wager [${wager.toString()}]: "
        if (input.trim() == '') {
            input_err = false
            input = wager
        } else {
            try {
                assert input.toInteger() > 0
                try {
                    assert input.toInteger() <= running_total
                    input_err = false
                    wager = input.toInteger()
                } catch (AssertionError ignored) {
                    printf 'Wager cannot be greater than your chip balance.'
                    double_tap()
                    println()
                }
            } catch (AssertionError ignored) {
                printf 'Wager must be greater than zero.'
                double_tap()
                println()
            } catch (ValueError) {
                printf 'Wager must be a whole number greater than zero.'
                double_tap()
                println()
            }
        }
    }
    return wager
}

def deal() {
    hands_played = 0
    hands = [] //[ [hand], hand score, hand ranks, soft ace, blackjack, surrendered, busted, split, wager ]
    hands[0] = [[],0,[],false,false,false,false,false,wager]
    hands[1] = [[],0,[],false,false,false,false,false,wager]
    deal_card(hands[1][0])
    deal_card(hands[0][0])
    deal_card(hands[1][0])
    deal_card(hands[0][0])
    return [hands, hands_played]
}

def deal_card(hand) {
    drawn_card = shoe.remove(0)
    if (drawn_card[0] == 'CC') {
        cut_card_drawn = true
        println 'The cut card has been drawn.' + '\7'
        hand.add(shoe.remove(0))
    } else {
        hand.add(drawn_card)
    }
    get_hand_info()
    return [hands, cut_card_drawn]
}

def show_start(hands, default_style) {
    printf "Dealer's Upcard: "
    style = colorize(hands[0][0][1][0])
    printf style + hands[0][0][1][0]
    printf default_style + ' '
    printf '\n'
    println()
    get_hand_info()
    printf 'Your Hand: '
    for (card in hands[1][0]) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '('
    if (hands[1][3] && hands[1][1] != 21) {
        printf 'soft '
    }
    printf hands[1][1] + ')'
    printf '\n'
}

def get_action(hands, hand_index, surrender_allowed) {
    if (hands[hand_index][1] == 21) {
        action = 'A'
    } else {
        wager_total = get_wager_total()
        options = ['Press <Enter> to stand or enter H to hit']
        valid_actions = ['', 'H']
        if (hands[hand_index][0].size() == 2) {
            if (!hands[hand_index][7] || (hands[hand_index][7] && double_allowed_after_split)) {
                if (running_total - wager_total - hands[hand_index][8] >= 0) {
                    options.add('D to double')
                    valid_actions.add('D')
                } else {
                    printf 'You do not have enough chips to double.'
                    double_tap()
                    println()
                }
            }
        }
        if (hands[hand_index][0].size() == 2) {
            if (hands[hand_index][0][0][0][0] == hands[hand_index][0][1][0][0]) {
                if (running_total - wager_total - hands[hand_index][8] >= 0) {
                    options.add('S to split')
                    valid_actions.add('S')
                } else {
                    printf 'You do not have enough chips to split.'
                    double_tap()
                    println()
                }
            }
        }
        if (surrender_allowed) {
            options.add('Q to surrender')
            valid_actions.add('Q')
        }
        prompt = ''
        if (options.size() > 1) {
            for (i = 0; i < (options.size() - 1); i++) {
                prompt += options[i] + ', '
            }
            prompt += 'or ' + options[options.size() - 1] + ': '
        } else {
            prompt = options[0] + ': '
        }
        action_err = true
        while (action_err) {
            action = System.console().readLine prompt
            action = action.trim().toUpperCase()
            if (action in valid_actions || action == '?') {
                action_err = false
            } else {
                println 'Invalid entry. Try again.'
            }
        }
    }
    return action
}

def check_strategy(hands, hand_index, surrender_allowed, action, num_decks, double_allowed_after_split) {
    correct_strategy = false
    // A,8
    if (hands[hand_index][0].size() == 2 && hands[hand_index][2] as Set == ['A','8'] as Set && num_decks == 1) {
        if (running_total - wager_total - hands[hand_index][8] >= 0) {
            rule = 'Stand on A & 8 when dealer shows 2-5, or 7 and higher. Double when 6.'
        } else {
            rule = 'Stand on A & 8 when dealer shows 2-5, or 7 and higher. Hit when 6 (unable to double).'
        }
        if (running_total - wager_total - hands[hand_index][8] < 0 \
          && hands[0][0][1][0][0] in ['2','3','4','5','7','8','9','0','J','Q','K','A'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] == '6' && (running_total - wager_total - hands[hand_index][8] >= 0 \
          && (!hands[hand_index][7] || (hands[hand_index][7] && double_allowed_after_split)) && action == 'D') \
          || running_total - wager_total - hands[hand_index][8] < 0 && action == 'H') {
            correct_strategy = true
        }
    }
    // A,7
    else if (hands[hand_index][0].size() == 2 && hands[hand_index][2] as Set == ['A','7'] as Set) {
        if (num_decks > 1) {
            if (running_total - wager_total - hands[hand_index][8] >= 0) {
                rule = 'Stand on A & 7 when dealer shows 2, or 7-8. Double when 3-6. Hit when 9 and higher.'
            } else {
                rule = 'Stand on A & 7 when dealer shows 2, or 7-8. Hit when 3-6 (unable to double), or 9 and higher.'
            }
            if (hands[0][0][1][0][0] in ['2','7','8'] && action == ''.toString()) {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['3','4','5','6'] \
              && (running_total - wager_total - hands[hand_index][8] >= 0 && action == 'D') \
              || running_total - wager_total - hands[hand_index][8] < 0 && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (running_total - wager_total - hands[hand_index][8] >= 0) {
                rule = 'Stand on A & 7 when dealer shows 2, 7-8, or A. Double when 3-6. Hit when 9-K.'
            } else {
                rule = 'Stand on A & 7 when dealer shows 2, 7-8, or A. Hit when 3-6 (unable to double), or 9-K.'
            }
            if (hands[0][0][1][0][0] in ['2','7','8','A'] && action == ''.toString()) {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['3','4','5','6'] \
              && (running_total - wager_total - hands[hand_index][8] >= 0 && (!hands[hand_index][7] \
              || (hands[hand_index][7] && double_allowed_after_split)) && action == 'D') \
              || running_total - wager_total - hands[hand_index][8] < 0 && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['9','0','J','Q','K'] && action == 'H') {
                correct_strategy = true
            }
        }
    }
    // A,6
    else if (hands[hand_index][0].size() == 2 && hands[hand_index][2] as Set == ['A','6'] as Set) {
        if (num_decks > 1) {
            if (running_total - wager_total - hands[hand_index][8] >= 0) {
                rule = 'Hit on A & 6 when dealer shows 2, or 7 and higher. Double when 3-6.'
            } else {
                rule = 'Always hit on A & 6 (unable to double when dealer shows 3-6).'
            }
            if (hands[0][0][1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['3','4','5','6'] \
              && (running_total - wager_total - hands[hand_index][8] >= 0 && action == 'D') \
              || running_total - wager_total - hands[hand_index][8] < 0 && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (running_total - wager_total - hands[hand_index][8] >= 0) {
                rule = 'Hit on A & 6 when dealer shows 7 and higher. Double when 2-6.'
            } else {
                rule = 'Always hit on A & 6 (unable to double when dealer shows 2-6).'
            }
            if (hands[0][0][1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['2','3','4','5','6'] \
              && (running_total - wager_total - hands[hand_index][8] >= 0 && (!hands[hand_index][7] \
              || (hands[hand_index][7] && double_allowed_after_split)) && action == 'D') \
              || running_total - wager_total - hands[hand_index][8] < 0 && action == 'H') {
                correct_strategy = true
            }
        }
    }
    // A,5
    else if (hands[hand_index][0].size() == 2 && hands[hand_index][2] as Set == ['A','5'] as Set \
      && running_total - wager_total - hands[hand_index][8] >= 0) {
        rule = 'Hit on A & 5 when dealer shows 2-3, or 7 and higher. Double when 4-6.'
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hand_index][7] || (hands[hand_index][7] \
          && double_allowed_after_split)) && action == 'D') {
            correct_strategy = true
        }
    }
    // A,4
    else if (hands[hand_index][0].size() == 2 && hands[hand_index][2] as Set == ['A','4'] as Set \
      && running_total - wager_total - hands[hand_index][8] >= 0) {
        rule = 'Hit on A & 4 when dealer shows 2-3, or 7 and higher. Double when 4-6.'
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hand_index][7] || (hands[hand_index][7] \
          && double_allowed_after_split)) && action == 'D') {
            correct_strategy = true
        }
    }
    // A,3
    else if (hands[hand_index][0].size() == 2 && hands[hand_index][2] as Set == ['A','3'] as Set \
      && running_total - wager_total - hands[hand_index][8] >= 0) {
        if (num_decks > 1) {
            rule = 'Hit on A & 3 when dealer shows 2-4, or 7 and higher. Double when 5-6.'
            if (hands[0][0][1][0][0] in ['2','3','4','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['5','6'] && action == 'D') {
                correct_strategy = true
            }
        } else {
            rule = 'Hit on A & 3 when dealer shows 2-3, or 7 and higher. Double when 4-6.'
            if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hand_index][7] || (hands[hand_index][7] \
              && double_allowed_after_split)) && action == 'D') {
                correct_strategy = true
            }
        }
    }
    // A,2
    else if (hands[hand_index][0].size() == 2 && hands[hand_index][2] as Set == ['A','2'] as Set \
      && running_total - wager_total - hands[hand_index][8] >= 0) {
        if (num_decks > 1) {
            rule = 'Hit on A & 2 when dealer shows 2-4, or 7 and higher. Double when 5-6.'
            if (hands[0][0][1][0][0] in ['2','3','4','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            } else if (hands[0][0][1][0][0] in ['5','6'] && action == 'D') {
                correct_strategy = true
            }
        } else {
            rule = 'Hit on A & 2 when dealer shows 2-3, or 7 and higher. Double when 4-6.'
            if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            } else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hand_index][7] || (hands[hand_index][7] \
              && double_allowed_after_split)) && action == 'D') {
                correct_strategy = true
            }
        }
    }
    // A,A
    else if (hands[hand_index][2] == ['A','A'] && running_total - wager_total - hands[hand_index][8] >= 0 \
      && action == 'S') {
            rule = 'Always split on A & A.'
            correct_strategy = true
    }
    // 9,9
    else if (hands[hand_index][2] == ['9','9'] && running_total - wager_total - hands[hand_index][8] >= 0) {
        rule = 'Split on 9 & 9 when dealer shows 2-6, or 8-9. Stand when 7, or 10 or higher.'
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','8','9'] && action == 'S') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','0','J','Q','K','A'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 8,8
    else if (hands[hand_index][2] == ['8','8'] && running_total - wager_total - hands[hand_index][8] >= 0 \
      && action == 'S') {
        rule = 'Always split on 8 & 8.'
        correct_strategy = true
    }
    // 7,7
    else if (hands[hand_index][2] == ['7','7'] && running_total - wager_total - hands[hand_index][8] >= 0) {
        if (num_decks > 1 || (num_decks ==1 && !double_allowed_after_split)) {
            rule = 'Split on 7 & 7 when dealer shows 2-7. Hit when 8 or higher.'
            if (hands[0][0][1][0][0] in ['2','3','4','5','6','7'] && action == 'S') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['8','9','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['0','J','Q','K'] && ((surrender_allowed && action == 'Q') \
              || (!surrender_allowed && action == 'H'))) {
                correct_strategy = true
            }
        } else {
            if (surrender_allowed) {
                rule = 'Split on 7 & 7 when dealer shows 2-8. Hit when 9 or A. Surrender when 10-K.'
            } else {
                rule = 'Split on 7 & 7 when dealer shows 2-8. Hit when 9 or A. Stand when 10-K.'
            }
            if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8'] && action == 'S') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['9','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['0','J','Q','K'] && ((surrender_allowed && action == 'Q') \
              || (!surrender_allowed && action == 'H'))) {
                correct_strategy = true
            }
        }
    }
    // 6,6
    else if (hands[hand_index][2] == ['6','6'] && running_total - wager_total - hands[hand_index][8] >= 0) {
        if (num_decks > 1) {
            rule = 'Split on 6 & 6 when dealer shows 3-6. Hit when 2, or 7 or higher.'
            if (hands[0][0][1][0][0] in ['3','4','5','6'] && action == 'S') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (double_allowed_after_split) {
                rule = 'Split on 6 & 6 when dealer shows 2-7. Hit when 8 or higher.'
                if (hands[0][0][1][0][0] in ['2','3','4','5','6','7'] && action == 'S') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
            } else {
                if (running_total - wager_total - hands[hand_index][8] >= 0) {
                    rule = 'Split on 6 & 6 when dealer shows 2-6. Hit when 7 or higher.'
                } else {
                    rule = 'Always hit on 6 & 6 (unable to split when dealer shows 2-6).'
                }
                if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == 'S') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
            }
        }
    }
    // 5,5
    else if (hands[hand_index][2] == ['5','5'] && num_decks > 1 && double_allowed_after_split \
      && running_total - wager_total - hands[hand_index][8] >= 0) {
        rule = 'Double on 5 & 5 when dealer shows 2-9. Hit when 10 or higher.'
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9'] && action == 'D') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 4,4
    else if (hands[hand_index][2] == ['4','4'] && running_total - wager_total - hands[hand_index][8] >= 0) {
        if (num_decks > 1) {
            if (double_allowed_after_split) {
                rule = 'Hit on 4 & 4 when dealer shows 2-4, or 7 or higher. Split when 5-6.'
                if (hands[0][0][1][0][0] in ['2','3','4','7','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['5','6'] && action == 'S') {
                    correct_strategy = true
                }
            } else {
                rule = 'Always hit on 4 & 4.'
                if (action == 'H') {
                    correct_strategy = true
                }
            }
        } else {
            if (double_allowed_after_split) {
                rule = 'Hit on 4 & 4 when dealer shows 2-3, or 7 or higher. Split when 4-6.'
                if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['4','5','6'] && action == 'S') {
                    correct_strategy = true
                }
            }
        }
    }
    // 3,3
    else if (hands[hand_index][2] == ['3','3'] && running_total - wager_total - hands[hand_index][8] >= 0) {
        if (num_decks > 1) {
            rule = 'Split on 3 & 3 when dealer shows 2-7. Hit when 8 or higher.'
            if (hands[0][0][1][0][0] in ['2','3','4','5','6','7'] && action == 'S') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (double_allowed_after_split) {
                rule = 'Split on 3 & 3 when dealer shows 2-8. Hit when 9 or higher.'
                if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8'] && action == 'S') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
            } else {
                rule = 'Hit on 3 & 3 when dealer shows 2-3, or 8 or higher Split when 4-7.'
                if (hands[0][0][1][0][0] in ['2','3','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['4','5','6','7'] && action == 'S') {
                    correct_strategy = true
                }
            }
        }
    }
    // 2,2
    else if (hands[hand_index][2] == ['2','2'] && running_total - wager_total - hands[hand_index][8] >= 0) {
        if (double_allowed_after_split) {
            if (num_decks > 1) {
                rule = 'Split on 2 & 2 when dealer shows 2-7. Hit when 8 or higher.'
                if (hands[0][0][1][0][0] in ['2','3','4','5','6','7'] && action == 'S') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
            } else {
                rule = 'Split on 2 & 2 when dealer shows 2, or 4-7. Hit when 3, or 8 or higher.'
                if (hands[0][0][1][0][0] in ['2','4','5','6','7'] && action == 'S') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['3','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
            }
        } else {
            if (num_decks > 1) {
                rule = 'Hit on 2 & 2 when dealer shows 2-3, or 8 or higher. Split when 4-7.'
                if (hands[0][0][1][0][0] in ['2','3','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['4','5','6','7'] && action == 'S') {
                    correct_strategy = true
                }
            } else {
                rule = 'Hit on 2 & 2 when dealer shows 2, or 8 or higher. Split when 3-7.'
                if (hands[0][0][1][0][0] in ['2','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['3','4','5','6','7'] && action == 'S') {
                    correct_strategy = true
                }
            }
        }
    }
    // hard 17 or more, soft 19 or more
    else if (hands[hand_index][1] >= 17) {
        if (!hands[hand_index][3]) {
            if (hands[hand_index][1] >= 17 && action == ''.toString()) {
                rule = 'Always stand on hard 17 or higher.'
                correct_strategy = true
            }
        } else {
            if (hands[hand_index][1] in [17,18] && action == 'H') {
                rule = 'Always hit on soft 17 or soft 18.'
                correct_strategy = true
            }
            else if (hands[hand_index][1] >= 19 && action == ''.toString()) {
                rule = 'Always stand on soft 19 or higher.'
                correct_strategy = true
            }
        }
    }
    // 16
    else if (hands[hand_index][1] == 16) {
        if (num_decks > 1) {
            if (surrender_allowed) {
                rule = 'Stand on 16 when dealer shows 2-6. Hit when 7-8. Surrender when 9 or higher.'
            } else {
                rule = 'Stand on 16 when dealer shows 2-6. Hit when 7 or higher.'
            }
            if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['7','8'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['9','0','J','Q','K','A'] && ((surrender_allowed && action == 'Q') \
              || (!surrender_allowed && action == 'H'))) {
                correct_strategy = true
            }
        } else {
            if (double_allowed_after_split) {
                if (surrender_allowed) {
                    rule = 'Stand on 16 when dealer shows 2-6. Hit when 7-9, or A. Surrender when 10.'
                } else {
                    rule = 'Stand on 16 when dealer shows 2-6. Hit when 7 or higher.'
                }
                if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['7','8','9','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['0','J','Q','K'] && ((surrender_allowed && action == 'Q') \
                  || (!surrender_allowed && action == 'H'))) {
                    correct_strategy = true
                }
            } else {
                if (surrender_allowed) {
                    rule = 'Stand on 16 when dealer shows 2-6. Hit when 7-9. Surrender when 10 or higher.'
                } else {
                    rule = 'Stand on 16 when dealer shows 2-6. Hit when 7 or higher.'
                }
                if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['7','8','9'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['0','J','Q','K','A'] && ((surrender_allowed && action == 'Q') \
                  || (!surrender_allowed && action == 'H'))) {
                    correct_strategy = true
                }

            }
        }
    }
    // 15
    else if (hands[hand_index][1] == 15) {
        if (surrender_allowed) {
            rule = 'Stand on 15 when dealer shows 2-6. Hit when 7-9 or A. Surrender when 10-K.'
        } else {
            rule = 'Stand on 15 when dealer shows 2-6. Hit when 7 or higher.'
        }
        if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','8','9','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['0','J','Q','K'] && ((surrender_allowed && action == 'Q') \
          || (!surrender_allowed && action == 'H'))) {
            correct_strategy = true
        }
    }
    // 13,14
    else if (hands[hand_index][1] in [13,14]) {
        rule = 'Stand on 13 or 14 when dealer shows 2-6. Hit when 7 or higher.'
        if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 12
    else if (hands[hand_index][1] == 12) {
        rule = 'Hit on 12 when dealer shows 2-3, or 7 or higher. Stand when 4-6.'
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 11
    else if (hands[hand_index][1] == 11) {
        if (num_decks > 1) {
            if (hands[hand_index][0].size() == 2) {
                if (running_total - wager_total - hands[hand_index][8] >= 0) {
                    rule = 'Double on 11 when dealer shows 2-K. Hit when A.'
                } else {
                    rule = 'Always hit on 11 (unable to double when dealer shows 2-K).'
                }
            } else {
                rule = 'Always hit on 11.'
            }
            if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9','0','J','Q','K']) {
                if (hands[hand_index][0].size() == 2 && (!hands[hand_index][7] || (hands[hand_index][7] \
                  && double_allowed_after_split)) && action == 'D' \
                  || (running_total - wager_total - hands[hand_index][8] < 0 \
                  || hands[hand_index][0].size() > 2) && action == 'H') {
                    correct_strategy = true
                }
            }
            else if (hands[0][0][1][0][0] == 'A' && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (hands[hand_index][0].size() == 2) {
                if (running_total - wager_total - hands[hand_index][8] >= 0) {
                    rule = 'Always double on 11.'
                } else {
                    rule = 'Always hit on 11 (unable to double).'
                }
            } else {
                rule = 'Always hit on 11.'
            }
            if (hands[hand_index][0].size() == 2 && (!hands[hand_index][7] || (hands[hand_index][7] \
              && double_allowed_after_split)) && action == 'D' \
              || (running_total - wager_total - hands[hand_index][8] < 0 \
              || hands[hand_index][0].size() > 2) && action == 'H') {
                correct_strategy = true
            }
        }
    }
    // 10
    else if (hands[hand_index][1] == 10) {
        if (hands[hand_index][0].size() == 2) {
            if (running_total - wager_total - hands[hand_index][8] >= 0) {
                rule = 'Double on 10 when dealer shows 2-9. Hit when 10 or higher.'
            } else {
                rule = 'Always hit on 10 (unable to double when dealer shows 2-9).'
            }
        } else {
            rule = 'Always hit on 10.'
        }
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9']) {
            if (running_total - wager_total - hands[hand_index][8] >= 0 && hands[hand_index][0].size() == 2 \
              && (!hands[hand_index][7] || (hands[hand_index][7] && double_allowed_after_split)) \
              && action == 'D' || ((hands[hand_index][0].size() > 2 \
              || running_total - wager_total - hands[hand_index][8] < 0) && action == 'H')) {
                correct_strategy = true
            }
        }
        else if (hands[0][0][1][0][0] in ['0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 9
    else if (hands[hand_index][1] == 9) {
        if (num_decks > 1) {
            if (hands[hand_index][0].size() == 2) {
                if (running_total - wager_total - hands[hand_index][8] >= 0) {
                    rule = 'Hit on 9 when dealer shows 2, or 7 or higher. Double when 3-6.'
                } else {
                    rule = 'Always hit on 9 (unable to double when dealer shows 3-6).'
                }
            } else {
                rule = 'Always hit on 9.'
            }
            if (hands[0][0][1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['3','4','5','6']) {
                if (running_total - wager_total - hands[hand_index][8] >= 0 && hands[hand_index][0].size() == 2 \
                  && (!hands[hand_index][7] || (hands[hand_index][7] && double_allowed_after_split)) \
                  && action == 'D' || ((hands[hand_index][0].size() > 2 \
                  || running_total - wager_total - hands[hand_index][8] < 0) && action == 'H')) {
                    correct_strategy = true
                }
            }
        } else {
            if (hands[hand_index][0].size() == 2) {
                if (running_total - wager_total - hands[hand_index][8] >= 0) {
                    rule = 'Double on 9 when dealer shows 2-6. Hit when 7 or higher.'
                } else {
                    rule = 'Always hit on 9 (unable to double when dealer shows 2-6).'
                }
            } else {
                rule = 'Always hit on 9.'
            }
            if (hands[0][0][1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['2','3','4','5','6']) {
                if (running_total - wager_total - hands[hand_index][8] >= 0 && hands[hand_index][0].size() == 2 \
                  && (!hands[hand_index][7] || (hands[hand_index][7] && double_allowed_after_split)) \
                  && action == 'D' || (hands[hand_index][0].size() > 2 \
                  || running_total - wager_total - hands[hand_index][8] < 0) && action == 'H') {
                    correct_strategy = true
                }
            }
        }
    }
    // 8 or less
    else if (hands[hand_index][1] <= 8) {
        if (num_decks > 1) {
            rule = 'Always hit on 8 or lower.'
            if (action == 'H') {
                correct_strategy = true
            }
        } else {
            if (hands[hand_index][1] == 8) {
                if (hands[hand_index][0].size() == 2) {
                    rule = 'Hit on 8 when dealer shows 2-4, or 7 or higher. Double when 5-6.'
                } else {
                    rule = 'Always hit on 8 or lower (unable to double when dealer shows 5-6).'
                }
                if (hands[0][0][1][0][0] in ['2','3','4','7','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['5','6']) {
                    if (running_total - wager_total - hands[hand_index][8] >= 0 && hands[hand_index][0].size() == 2 \
                      && (!hands[hand_index][7] || (hands[hand_index][7] && double_allowed_after_split)) \
                      && action == 'D' || running_total - wager_total - hands[hand_index][8] < 0 \
                      && hands[hand_index][0].size() > 2 && action == 'H') {
                        correct_strategy = true
                    }
                }
            } else {
                rule = 'Always hit on 7 or lower.'
                if (action == 'H') {
                    correct_strategy = true
                }
            }
        }
    }
    return [correct_strategy, rule]
}

def take_action(hands, action, cut_card_drawn) {
    if (action == 'H') {
        (hands, cut_card_drawn) = deal_card(hands[hand_index][0])
    }
    else if (action == 'D') {
        hands[hand_index][8] = 2 * hands[hand_index][8]
        println "You have increased your wager to " + hands[hand_index][8]
        println()
        (hands, cut_card_drawn) = deal_card(hands[hand_index][0])
    }
    else if (action == 'S') {
        println "You have increased your wager by " + wager
        println()
        split_card = hands[hand_index][0][1]
        hands[hand_index][0].remove(split_card)
        hands[hand_index + 1] = [[],0,[],false,false,false,false,false,wager]
        hands[hand_index + 1][0].add(split_card)
        (hands, cut_card_drawn) = deal_card(hands[hand_index][0])
        (hands, cut_card_drawn) = deal_card(hands[hand_index + 1][0])
    }
    get_hand_info()
    return [hands, cut_card_drawn]
}

def get_hand_info() {
    i = 0
    for (hand in hands) {
        hand_score = 0
        hand_ranks = []
        soft_ace = false
        for (card in hand[0]) {
            hand_score += card[1]
            hand_ranks.add(card[0][0])
        }
        for (rank in hand_ranks) {
            if (rank == 'A') {
                if (hand_score > 21) {
                    hand_score -= 10
                } else {
                    soft_ace = true
                }
            }
        }
        hands[i][1] = hand_score
        hands[i][2] = hand_ranks
        hands[i][3] = soft_ace
        i += 1
    }
}

def get_wager_total() {
    wager_total = 0
    for (int i = 1;i<hands.size();i++) {
        wager_total += hands[i][8]
    }
    return wager_total
}

def results(hands, running_total) {
    println()
    proceeds = 0.0
    for (int i = 1;i<hands.size();i++) {
        if (hands.size() > 2) {
            printf 'Hand ' + i +': '
        }
        if ((hands[0][1] > 21 || (hands[0][1] < hands[i][1]) && !hands[i][5] && !hands[i][6])) {
            printf 'Winner!!!' + '\7'
            if (hands[i][4]) {
                running_total += (hands[i][8] * 1.5).round(2)
                proceeds += (hands[i][8] * 1.5).round(2)
            } else {
                running_total += hands[i][8]
                proceeds += hands[i][8]
            }
        }
        else if (hands[i][5]) {
            printf 'Your wager has been split with the house.'
            running_total -= (hands[i][8] / 2).round(2)
            proceeds -= (hands[i][8] / 2).round(2)
        }
        else if (hands[0][1] > hands[i][1] || hands[i][6]) {
            printf 'The house wins.'
            if (!hands[hand_index][6]) {
                double_tap()
            }
            running_total -= hands[i][8]
            proceeds -= hands[i][8]
        }
        else if (hands[0][1] == hands[i][1]) {
            printf 'Push'
        }
        println()
    }
    cash = proceeds - proceeds.intValue()
    if (cash > 0) {
        println 'Total proceeds: ' + proceeds.intValue().toString() + ' chips and $' + cash.toString()
    } else {
        println 'Total proceeds: ' + proceeds.intValue().toString()
    }
    return running_total
}

def show_hand(hands, default_style) {
    if (hands.size() > 2) {
        printf 'Your Hand ' + hand_index +': '
    } else {
        printf 'Your Hand: '
    }
    for (card in hands[hand_index][0]) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '('
    if (hands[hand_index][3] && hands[hand_index][1] != 21) {
        printf 'soft '
    }
    printf hands[hand_index][1] + ')'
    printf '\n'
    if (hands.size() > 2) {
        println()
    }
}

def show_dealers_hand(hands, default_style) {
    printf "Dealer's Hand: "
    for (card in hands[0][0]) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '('
    if (hands[0][3] && hands[0][1] != 21) {
        printf 'soft '
    }
    printf hands[0][1] + ')'
    printf '\n'
}

def dealer_hits(hands, default_style, dealer_stands_soft17, cut_card_drawn) {
    show_dealers_hand(hands, default_style)
    if (!hands[hand_index][4] && !hands[hand_index][5] && !hands[hand_index][6]) {
        while (hands[0][1] < 17 || (!dealer_stands_soft17 && hands[0][1] == 17 && hands[0][3])) {
            sleep(1000)
            println 'Dealer hits.'
            (hands, cut_card_drawn) = deal_card(hands[0][0])
            get_hand_info()
            show_dealers_hand(hands, default_style)
        }
    }
    if (hands[0][1] > 21) {
        println 'Dealer busts!'
    }
    return cut_card_drawn
}

def reaction(hands, cut_card_drawn, action) {
    if (action == 'A') {
        if (hands[hand_index][0].size() == 2) {
            hands[hand_index][4] = true
            println 'Blackjack!'
        }
        println '\7'
        sleep(1000)
    }
    else if (action == '?') {
        (correct_strategy, rule) = check_strategy(hands, hand_index, surrender_allowed, action, num_decks, double_allowed_after_split)
        println rule
    } else {
        (correct_strategy, rule) = check_strategy(hands, hand_index, surrender_allowed, action, num_decks, double_allowed_after_split)
        total_attempts += 1
        if (correct_strategy) {
            accurate_attempts += 1
            println 'Your strategy is correct.'
            println rule
        } else {
            println 'Your strategy is incorrect.'
            printf rule
            double_tap()
            println()
        }
        println()
        if (action == 'Q') {
            hands[hand_index][5] =true
            println 'You have surrendered your hand.'
            println()
        } else {
            if (action == 'S') {
                hands[hand_index][7] = true
                (hands, cut_card_drawn) = take_action(hands, action, cut_card_drawn)
            } else if (action in ['H', 'D']) {
                (hands, cut_card_drawn) = take_action(hands, action, cut_card_drawn)
            }
            if (hands[hand_index][1] > 21) {
                hands[hand_index][6] =true
                printf 'You bust!'
                double_tap()
                println()
            }
        }
        show_hand(hands, default_style)
    }
    return [hands, cut_card_drawn]
}

def show_outcome() {
    if (running_total == 0) {
        println 'You leave with nothing. Play again if you dare!'
    }
    else if (running_total.intValue() == 0 || running_total > 0) {
        println 'Credit balance of $' + String.format("%.2f", running_total) + '. Cash dispensed below.'
    } else {
        println 'Balance due $' + String.format("%.2f", running_total) + '. Insert credit card below.'
    }
}

def end_of_game(play_again, running_total) {
    println 'Game over.'
    again_input_err = true
    if (running_total.intValue() != 0) {
        while (again_input_err) {
            again_yn = System.console().readLine 'Press <Enter> to play again or enter Q to quit: '
            if (again_yn.trim() == '') {
                again_input_err = false
                play_again = true
                System.out.print("\033[H\033[2J")
                System.out.flush()
            }
            else if (again_yn.toUpperCase() == 'Q') {
                again_input_err = false
                play_again = false
                show_outcome()
            } else {
                printf 'Try again. '
                double_tap()
                println()
            }
        }
    } else {
        play_again = false
        show_outcome()
    }
    return play_again
}

def colorize(card) {
    if (card[1] == 'C') { style = "${(char)27}[34;40"+'m' }
    else if (card[1] == 'D') { style = "${(char)27}[91;40"+'m' }
    else if (card[1] == 'H') { style = "${(char)27}[31;40"+'m' }
    else if (card[1] == 'S') { style = "${(char)27}[94;40"+'m' }
    return style
}

def double_tap() {
    printf '\7'
    sleep(200)
    printf '\7'
}

def mainMethod() {
    (default_style, running_total, num_decks, dealer_stands_soft17, double_allowed_after_split, surrender_allowed, total_attempts, accurate_attempts) = init_game()
    (shoe, cut_card_drawn) = init_shoe(num_decks)
    play_again = true

    while (play_again) {
        wager = init_wager(running_total, total_attempts, accurate_attempts)
        hand_index = 1
        if (cut_card_drawn) {
            (shoe, cut_card_drawn) = init_shoe(num_decks)
        }
        (hands, hands_played) = deal()
        show_start(hands, default_style)
        while (hands_played < hand_index) {
            action = get_action(hands, hand_index, surrender_allowed)
            (hands, cut_card_drawn) = reaction(hands, cut_card_drawn, action)
            while (action !in ['','A','D','Q'] && !hands[hand_index][5] && !hands[hand_index][6]) {
                action = get_action(hands, hand_index, surrender_allowed)
                (hands, cut_card_drawn) = reaction(hands, cut_card_drawn, action)
            }
            hands_played += 1
            if (hands[hand_index][7]) {
                hand_index += 1
                show_hand(hands, default_style)
            }
        }
        cut_card_drawn = dealer_hits(hands, default_style, dealer_stands_soft17, cut_card_drawn)
        running_total = results(hands, running_total)
        if (accurate_attempts != total_attempts) {
            println 'Game over.'
            double_tap()
            println 'Your accuracy has fallen below 100%.'
            play_again = false
            show_outcome()
        } else {
            play_again = end_of_game(play_again, running_total)
        }
    }
}

mainMethod()
