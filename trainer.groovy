#!/usr/bin/env groovy

/* Blackjack trainer
   =================
   Options
   1. [Multiple (2, 4, 6, 8) | Single] deck(s).
   2. Dealer [stands | hits] on soft 17.
   3. Double on any 2 cards. Double [allowed | not allowed] after split.
   4. Surrender [allowed | not allowed]. */

import groovy.json.JsonSlurper

def init_game() {
    def inputFile = new File("config.json")
    def config = new JsonSlurper().parseText(inputFile.text)
    default_style = "${(char)27}[37;40m"
    min_wager = config.min_wager
    credit_limit = config.credit_limit
    num_decks = config.num_decks
    dealer_stands_soft17 = config.dealer_stands_soft17
    double_allowed_after_split = config.double_allowed_after_split
    surrender_allowed = config.surrender_allowed
    complete_accuracy = config.complete_accuracy
    sounds = config.sounds
    display_winloss_stat = config.display_winloss_stat
    display_doubled_winloss_stat = config.display_doubled_winloss_stat
    display_blackjack_stat = config.display_blackjack_stat

    total_attempts = 0
    accurate_attempts = 0
    hands_won = 0
    hands_push = 0
    hands_lost = 0
    hands_surrender = 0
    doubled_hands_won = 0
    doubled_hands_push = 0
    doubled_hands_lost = 0
    doubled_hands_surrender = 0
    blackjack_won = 0
    blackjack_push = 0
    gambler_account = 0
    gambler_chips_cash = 0
    clear_screen()
    println '*************************************'
    println '*                                   *'
    println '*   Welcome to Blackjack Trainer!   *'
    println '*                                   *'
    println '*************************************'
    println()
    while (gambler_account < min_wager) {
        make_sound('Tink.aiff')
        System.console().readLine 'You must have at least ' + min_wager.toString() + \
          ' chips to wager. Press any key to continue to Cashier. '
        println()
        (gambler_account, gambler_chips_cash) = cashier("B")
    }
    return [default_style, gambler_account, gambler_chips_cash, num_decks, dealer_stands_soft17, \
      double_allowed_after_split, surrender_allowed, total_attempts, accurate_attempts, hands_won, hands_push, \
      hands_lost, hands_surrender, doubled_hands_won, doubled_hands_push, doubled_hands_lost, doubled_hands_surrender, \
      blackjack_won, blackjack_push]
}

def clear_screen() {
    System.out.print("\033[H\033[2J")
    System.out.flush()
}

def cashier(cashier_action) {
    credit_avail = credit_limit - gambler_account
    standings()
    if (cashier_action == 'E') {
        if (credit_avail > 0 && gambler_chips_cash.intValue() > 0 && gambler_account > 0) {
            cashier_action_err = true
            while (cashier_action_err) {
                cashier_action = System.console().readLine 'Buy (B) or Sell (S) chips? or Quit (Q): '
                cashier_action = cashier_action.trim().toUpperCase()
                if (cashier_action in ['B', 'S', 'Q']) {
                    cashier_action_err = false
                } else {
                    println 'Invalid entry. Try again.'
                    make_sound('Hero.aiff')
                    println()
                }
            }
        }
        else if (gambler_chips_cash.intValue() > 0 && gambler_account > 0) {
            cashier_action = 'S'
        }
        else if (credit_avail > 0) {
            cashier_action = 'B'
        }
    }
    if (cashier_action == 'B') {
        units_err = true
        units_quit = false
        while (units_err) {
            units = System.console().readLine 'Chips to buy on account [1 to ' + credit_avail.toString() + \
              '] or Quit (Q): '
            units = units.trim()
            if (units.toUpperCase() == 'Q') {
                units_err = false
                units_quit = true
            } else {
                try {
                    assert units.toInteger() > 0
                    try {
                        assert units.toInteger() <= credit_avail
                        units_err = false
                        units = units.toInteger()
                    } catch (AssertionError ignored) {
                        printf 'Chips cannot be greater than the available credit.'
                        make_sound('Hero.aiff')
                        println()
                    }
                } catch (AssertionError ignored) {
                    printf 'Chips must be greater than zero.'
                    make_sound('Hero.aiff')
                    println()
                } catch (ValueError) {
                    printf 'Chips must be a whole number greater than zero.'
                    make_sound('Hero.aiff')
                    println()
                }
            }
        }
        println()
        if (!units_quit) {
            gambler_account += units
            gambler_chips_cash += units
            make_sound('Bottle.aiff')
            sleep(500)
        }
    }
    else if (cashier_action == 'S') {
        balance = gambler_chips_cash - gambler_account
        if (gambler_chips_cash > 0) {
            if (balance > 0) {
                max_sell = gambler_account
            } else {
                max_sell = gambler_chips_cash.intValue()
            }
            units_err = true
            units_quit = false
            while (units_err) {
                units = System.console().readLine 'Chips to sell [1 to ' + max_sell.toString() + '] or Quit (Q): '
                units = units.trim()
                if (units.toUpperCase() == 'Q') {
                    units_err = false
                    units_quit = true
                } else {
                    try {
                        assert units.toInteger() > 0
                        try {
                            assert units.toInteger() <= max_sell
                            units_err = false
                            units = units.toInteger()
                        } catch (AssertionError ignored) {
                            printf 'Chips cannot be greater than the available chips.'
                            make_sound('Hero.aiff')
                            println()
                        }
                    } catch (AssertionError ignored) {
                        printf 'Chips must be greater than zero.'
                        make_sound('Hero.aiff')
                        println()
                    } catch (ValueError) {
                        printf 'Chips must be a whole number greater than zero.'
                        make_sound('Hero.aiff')
                        println()
                    }
                }
            }
            println()
            if (!units_quit) {
                gambler_account -= units
                gambler_chips_cash -= units
                make_sound('Bottle.aiff')
                sleep(500)
            }
        }
    }
    return [gambler_account, gambler_chips_cash]
}

def standings() {
    gambler_balance = gambler_chips_cash - gambler_account
    cash = gambler_chips_cash - gambler_chips_cash.intValue()
    gam_bal_string = '$' + gambler_balance.toString()
    gam_acc_string = '$' + gambler_account.toString()
    cash_string = '$' + cash.toString()
    if (gambler_chips_cash.intValue().toString().length() <= 4) {
        chips_pad = 5
    } else {
        chips_pad = gambler_chips_cash.intValue().toString().length() + 1
    }
    if (java.text.NumberFormat.currencyInstance.format(cash).length() <= 5) {
        cash_pad = 7
    } else {
        cash_pad = java.text.NumberFormat.currencyInstance.format(cash).length() + 2
    }
    if (java.text.NumberFormat.currencyInstance.format(gambler_account).length() <= 7) {
        account_pad = 9
    } else {
        account_pad = java.text.NumberFormat.currencyInstance.format(gambler_account).length() + 2
    }
    if (java.text.NumberFormat.currencyInstance.format(gambler_balance).length() <= 7) {
        balance_pad = 9
    } else {
        balance_pad = java.text.NumberFormat.currencyInstance.format(gambler_balance).length() + 2
    }

    if (cash > 0) {
        println 'Chips'.padLeft(chips_pad) + 'Cash'.padLeft(cash_pad) + 'Account'.padLeft(account_pad) + 'Balance'.padLeft(balance_pad)
    } else {
        println 'Chips'.padLeft(chips_pad) + 'Account'.padLeft(account_pad) + 'Balance'.padLeft(balance_pad)
    }
    if (gambler_chips_cash >= 0) {
        printf "${(char)27}[32;40"+'m' + gambler_chips_cash.intValue().toString().padLeft(chips_pad)
    } else {
        printf "${(char)27}[31;40"+'m' + gambler_chips_cash.intValue().toString().padLeft(chips_pad)
    }
    if (cash > 0) {
        printf "${(char)27}[32;40"+'m' + java.text.NumberFormat.currencyInstance.format(cash).padLeft(7)
    }
    if (gambler_account >= 0) {
        printf "${(char)27}[32;40"+'m' + java.text.NumberFormat.currencyInstance.format(gambler_account).padLeft(account_pad)
    } else {
        printf "${(char)27}[31;40"+'m' + java.text.NumberFormat.currencyInstance.format(gambler_account).padLeft(account_pad)
    }
    if (gambler_balance >= 0) {
        printf "${(char)27}[32;40"+'m' + java.text.NumberFormat.currencyInstance.format(gambler_balance).padLeft(balance_pad)
    } else {
        printf "${(char)27}[31;40"+'m' + java.text.NumberFormat.currencyInstance.format(gambler_balance).padLeft(balance_pad)
    }
    println default_style
    println()
    if (display_winloss_stat) {
        winloss(hands_won, hands_lost, hands_surrender, hands_push, false)
        if (!display_doubled_winloss_stat && !display_blackjack_stat) {
            println()
        }
    }
    if (display_doubled_winloss_stat) {
        winloss(doubled_hands_won, doubled_hands_lost, doubled_hands_surrender, doubled_hands_push, true)
        if (!display_blackjack_stat) {
            println()
        }
    }
    if (display_blackjack_stat) {
        blackjack_stat()
        println()
    }
}

def init_shoe() {
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
    cut_index = Math.abs( new Random().nextInt() % (num_cards - (num_cards * .75).intValue()) ) + \
      (num_cards * .75).intValue() - 5
    shoe.addAll(cut_index,[['CC',0]])
    cut_card_drawn = false
    println 'The shoe has been shuffled.'
    make_sound('Purr.aiff')
    return [shoe, cut_card_drawn]
}

def init_wager() {
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
    if (!complete_accuracy && total_attempts != 0) {
        println 'Accuracy: ' + (100 * (accurate_attempts / total_attempts)).round(1).toString() + '%'
    }
    standings()
    try {
        wager
    } catch (MissingPropertyException) {
        if (gambler_chips_cash >= min_wager) {
            wager = min_wager
        } else {
            wager = gambler_chips_cash.intValue()
        }
    }
    if (wager > gambler_chips_cash) {
        wager = gambler_chips_cash.intValue()
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
                    assert input.toInteger() <= gambler_chips_cash
                    try {
                        assert input.toInteger() >= min_wager
                        input_err = false
                        wager = input.toInteger()
                    } catch (AssertionError ignored) {
                        printf 'Wager must be a minimum of ' + min_wager.toString() + '.'
                        make_sound('Hero.aiff')
                        println()
                    }
                } catch (AssertionError ignored) {
                    printf 'Wager cannot be greater than your chip balance.'
                    make_sound('Hero.aiff')
                    println()
                }
            } catch (AssertionError ignored) {
                printf 'Wager must be greater than zero.'
                make_sound('Hero.aiff')
                println()
            } catch (ValueError) {
                printf 'Wager must be a whole number greater than zero.'
                make_sound('Hero.aiff')
                println()
            }
        }
    }
    return wager
}

def deal() {
    hands_played = 0
    hands = [] //[ hand, hand score, hand ranks, soft ace, blackjack, surrendered, busted, split, doubled, wager ]
    hands[0] = [[],0,[],false,false,false,false,false,false,wager]
    hands[1] = [[],0,[],false,false,false,false,false,false,wager]
    deal_card(hands[1][0])
    deal_card(hands[0][0])
    deal_card(hands[1][0])
    deal_card(hands[0][0])
    get_hand_info()
    return hands_played
}

def deal_card(hand) {
    drawn_card = shoe.remove(0)
    if (drawn_card[0] == 'CC') {
        cut_card_drawn = true
        println 'The cut card has been drawn.'
        make_sound('Pop.aiff')
        hand.add(shoe.remove(0))
    } else {
        hand.add(drawn_card)
    }
    return cut_card_drawn
}

def show_start() {
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

def get_action() {
    if (hands[hands_index][1] == 21) {
        action = 'A'
    } else {
        wager_total = get_wager_total()
        options = ['Press <Enter> to stand or enter H to hit']
        valid_actions = ['', 'H']
        if (hands[hands_index][0].size() == 2) {
            if (!hands[hands_index][7] || (hands[hands_index][7] && double_allowed_after_split)) {
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                    options.add('D to double')
                    valid_actions.add('D')
                } else {
                    printf 'You do not have enough chips to double.'
                    make_sound('Tink.aiff')
                    println()
                }
            }
        }
        if (hands[hands_index][0].size() == 2) {
            if (hands[hands_index][0][0][0][0] == hands[hands_index][0][1][0][0]) {
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                    options.add('S to split')
                    valid_actions.add('S')
                } else {
                    printf 'You do not have enough chips to split.'
                    make_sound('Tink.aiff')
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
                make_sound('Hero.aiff')
            }
        }
    }
    if (action == 'D') {
        hands[hands_index][8] = true
    }
    return action
}

def check_strategy() {
    correct_strategy = false
    // A,8
    if (hands[hands_index][0].size() == 2 && hands[hands_index][2] as Set == ['A','8'] as Set && num_decks == 1) {
        if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
            rule = 'Stand on A & 8 when dealer shows 2-5, or 7 and higher. Double when 6.'
        } else {
            rule = 'Stand on A & 8 when dealer shows 2-5, or 7 and higher. Hit when 6 (unable to double).'
        }
        if (gambler_chips_cash - wager_total - hands[hands_index][9] < 0 \
          && hands[0][0][1][0][0] in ['2','3','4','5','7','8','9','0','J','Q','K','A'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] == '6' && (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 \
          && (!hands[hands_index][7] || (hands[hands_index][7] && double_allowed_after_split)) && action == 'D') \
          || gambler_chips_cash - wager_total - hands[hands_index][9] < 0 && action == 'H') {
            correct_strategy = true
        }
    }
    // A,7
    else if (hands[hands_index][0].size() == 2 && hands[hands_index][2] as Set == ['A','7'] as Set) {
        if (num_decks > 1) {
            if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                rule = 'Stand on A & 7 when dealer shows 2, or 7-8. Double when 3-6. Hit when 9 and higher.'
            } else {
                rule = 'Stand on A & 7 when dealer shows 2, or 7-8. Hit when 3-6 (unable to double), or 9 and higher.'
            }
            if (hands[0][0][1][0][0] in ['2','7','8'] && action == ''.toString()) {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['3','4','5','6'] \
              && (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 && action == 'D') \
              || gambler_chips_cash - wager_total - hands[hands_index][9] < 0 && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                rule = 'Stand on A & 7 when dealer shows 2, 7-8, or A. Double when 3-6. Hit when 9-K.'
            } else {
                rule = 'Stand on A & 7 when dealer shows 2, 7-8, or A. Hit when 3-6 (unable to double), or 9-K.'
            }
            if (hands[0][0][1][0][0] in ['2','7','8','A'] && action == ''.toString()) {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['3','4','5','6'] \
              && (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 && (!hands[hands_index][7] \
              || (hands[hands_index][7] && double_allowed_after_split)) && action == 'D') \
              || gambler_chips_cash - wager_total - hands[hands_index][9] < 0 && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['9','0','J','Q','K'] && action == 'H') {
                correct_strategy = true
            }
        }
    }
    // A,6
    else if (hands[hands_index][0].size() == 2 && hands[hands_index][2] as Set == ['A','6'] as Set) {
        if (num_decks > 1) {
            if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                rule = 'Hit on A & 6 when dealer shows 2, or 7 and higher. Double when 3-6.'
            } else {
                rule = 'Always hit on A & 6 (unable to double when dealer shows 3-6).'
            }
            if (hands[0][0][1][0][0] in ['2','7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['3','4','5','6'] \
              && (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 && action == 'D') \
              || gambler_chips_cash - wager_total - hands[hands_index][9] < 0 && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                rule = 'Hit on A & 6 when dealer shows 7 and higher. Double when 2-6.'
            } else {
                rule = 'Always hit on A & 6 (unable to double when dealer shows 2-6).'
            }
            if (hands[0][0][1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
                correct_strategy = true
            }
            else if (hands[0][0][1][0][0] in ['2','3','4','5','6'] \
              && (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 && (!hands[hands_index][7] \
              || (hands[hands_index][7] && double_allowed_after_split)) && action == 'D') \
              || gambler_chips_cash - wager_total - hands[hands_index][9] < 0 && action == 'H') {
                correct_strategy = true
            }
        }
    }
    // A,5
    else if (hands[hands_index][0].size() == 2 && hands[hands_index][2] as Set == ['A','5'] as Set \
      && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
        rule = 'Hit on A & 5 when dealer shows 2-3, or 7 and higher. Double when 4-6.'
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hands_index][7] || (hands[hands_index][7] \
          && double_allowed_after_split)) && action == 'D') {
            correct_strategy = true
        }
    }
    // A,4
    else if (hands[hands_index][0].size() == 2 && hands[hands_index][2] as Set == ['A','4'] as Set \
      && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
        rule = 'Hit on A & 4 when dealer shows 2-3, or 7 and higher. Double when 4-6.'
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hands_index][7] || (hands[hands_index][7] \
          && double_allowed_after_split)) && action == 'D') {
            correct_strategy = true
        }
    }
    // A,3
    else if (hands[hands_index][0].size() == 2 && hands[hands_index][2] as Set == ['A','3'] as Set \
      && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
            else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hands_index][7] || (hands[hands_index][7] \
              && double_allowed_after_split)) && action == 'D') {
                correct_strategy = true
            }
        }
    }
    // A,2
    else if (hands[hands_index][0].size() == 2 && hands[hands_index][2] as Set == ['A','2'] as Set \
      && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
            } else if (hands[0][0][1][0][0] in ['4','5','6'] && (!hands[hands_index][7] || (hands[hands_index][7] \
              && double_allowed_after_split)) && action == 'D') {
                correct_strategy = true
            }
        }
    }
    // A,A
    else if (hands[hands_index][2] == ['A','A'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 \
      && (action == 'S' || action == '?')) {
            rule = 'Always split on A & A.'
            correct_strategy = true
    }
    // 9,9
    else if (hands[hands_index][2] == ['9','9'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
        rule = 'Split on 9 & 9 when dealer shows 2-6, or 8-9. Stand when 7, or 10 or higher.'
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','8','9'] && action == 'S') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','0','J','Q','K','A'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 8,8
    else if (hands[hands_index][2] == ['8','8'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 \
      && (action == 'S' || action == '?')) {
        rule = 'Always split on 8 & 8.'
        correct_strategy = true
    }
    // 7,7
    else if (hands[hands_index][2] == ['7','7'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
    else if (hands[hands_index][2] == ['6','6'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
    else if (hands[hands_index][2] == ['5','5'] && num_decks > 1 && double_allowed_after_split \
      && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
        rule = 'Double on 5 & 5 when dealer shows 2-9. Hit when 10 or higher.'
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9'] && action == 'D') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 4,4
    else if (hands[hands_index][2] == ['4','4'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
    else if (hands[hands_index][2] == ['3','3'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
    else if (hands[hands_index][2] == ['2','2'] && gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
    // soft 19 or more
    else if (hands[hands_index][1] >= 19 && hands[hands_index][3] && (action == ''.toString() || action == '?')) {
        rule = 'Always stand on soft 19 or higher.'
        correct_strategy = true
    }
    // soft 17 or soft 18
    else if (hands[hands_index][1] in [17,18] && hands[hands_index][3] && (action == 'H' || action == '?')) {
        rule = 'Always hit on soft 17 or soft 18.'
        correct_strategy = true
    }
    // hard 17 or more
    else if (hands[hands_index][1] >= 17 && !hands[hands_index][3]) {
        if (hands[hands_index][1] >= 17 && (action == ''.toString() || action == '?')) {
            rule = 'Always stand on hard 17 or higher.'
            correct_strategy = true
        }
    }
    // 16
    else if (hands[hands_index][1] == 16) {
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
    else if (hands[hands_index][1] == 15) {
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
    else if (hands[hands_index][1] in [13,14]) {
        rule = 'Stand on 13 or 14 when dealer shows 2-6. Hit when 7 or higher.'
        if (hands[0][0][1][0][0] in ['2','3','4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 12
    else if (hands[hands_index][1] == 12) {
        rule = 'Hit on 12 when dealer shows 2-3, or 7 or higher. Stand when 4-6.'
        if (hands[0][0][1][0][0] in ['2','3','7','8','9','0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
        else if (hands[0][0][1][0][0] in ['4','5','6'] && action == ''.toString()) {
            correct_strategy = true
        }
    }
    // 11
    else if (hands[hands_index][1] == 11) {
        if (num_decks > 1) {
            if (hands[hands_index][0].size() == 2) {
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                    rule = 'Double on 11 when dealer shows 2-K. Hit when A.'
                } else {
                    rule = 'Always hit on 11 (unable to double when dealer shows 2-K).'
                }
            } else {
                rule = 'Always hit on 11.'
            }
            if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9','0','J','Q','K']) {
                if (hands[hands_index][0].size() == 2 && (!hands[hands_index][7] || (hands[hands_index][7] \
                  && double_allowed_after_split)) && action == 'D' \
                  || (gambler_chips_cash - wager_total - hands[hands_index][9] < 0 \
                  || hands[hands_index][0].size() > 2) && action == 'H') {
                    correct_strategy = true
                }
            }
            else if (hands[0][0][1][0][0] == 'A' && action == 'H') {
                correct_strategy = true
            }
        } else {
            if (hands[hands_index][0].size() == 2) {
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                    rule = 'Always double on 11.'
                } else {
                    rule = 'Always hit on 11 (unable to double).'
                }
            } else {
                rule = 'Always hit on 11.'
            }
            if (hands[hands_index][0].size() == 2 && (!hands[hands_index][7] || (hands[hands_index][7] \
              && double_allowed_after_split)) && action == 'D' \
              || (gambler_chips_cash - wager_total - hands[hands_index][9] < 0 \
              || hands[hands_index][0].size() > 2) && action == 'H') {
                correct_strategy = true
            }
        }
    }
    // 10
    else if (hands[hands_index][1] == 10) {
        if (hands[hands_index][0].size() == 2) {
            if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
                rule = 'Double on 10 when dealer shows 2-9. Hit when 10 or higher.'
            } else {
                rule = 'Always hit on 10 (unable to double when dealer shows 2-9).'
            }
        } else {
            rule = 'Always hit on 10.'
        }
        if (hands[0][0][1][0][0] in ['2','3','4','5','6','7','8','9']) {
            if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 && hands[hands_index][0].size() == 2 \
              && (!hands[hands_index][7] || (hands[hands_index][7] && double_allowed_after_split)) \
              && action == 'D' || ((hands[hands_index][0].size() > 2 \
              || gambler_chips_cash - wager_total - hands[hands_index][9] < 0) && action == 'H')) {
                correct_strategy = true
            }
        }
        else if (hands[0][0][1][0][0] in ['0','J','Q','K','A'] && action == 'H') {
            correct_strategy = true
        }
    }
    // 9
    else if (hands[hands_index][1] == 9) {
        if (num_decks > 1) {
            if (hands[hands_index][0].size() == 2) {
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 && hands[hands_index][0].size() == 2 \
                  && (!hands[hands_index][7] || (hands[hands_index][7] && double_allowed_after_split)) \
                  && action == 'D' || ((hands[hands_index][0].size() > 2 \
                  || gambler_chips_cash - wager_total - hands[hands_index][9] < 0) && action == 'H')) {
                    correct_strategy = true
                }
            }
        } else {
            if (hands[hands_index][0].size() == 2) {
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0) {
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
                if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 && hands[hands_index][0].size() == 2 \
                  && (!hands[hands_index][7] || (hands[hands_index][7] && double_allowed_after_split)) \
                  && action == 'D' || (hands[hands_index][0].size() > 2 \
                  || gambler_chips_cash - wager_total - hands[hands_index][9] < 0) && action == 'H') {
                    correct_strategy = true
                }
            }
        }
    }
    // 8 or less
    else if (hands[hands_index][1] <= 8) {
        if (num_decks > 1) {
            rule = 'Always hit on 8 or lower.'
            if (action == 'H') {
                correct_strategy = true
            }
        } else {
            if (hands[hands_index][1] == 8) {
                if (hands[hands_index][0].size() == 2) {
                    rule = 'Hit on 8 when dealer shows 2-4, or 7 or higher. Double when 5-6.'
                } else {
                    rule = 'Always hit on 8 or lower (unable to double when dealer shows 5-6).'
                }
                if (hands[0][0][1][0][0] in ['2','3','4','7','8','9','0','J','Q','K','A'] && action == 'H') {
                    correct_strategy = true
                }
                else if (hands[0][0][1][0][0] in ['5','6']) {
                    if (gambler_chips_cash - wager_total - hands[hands_index][9] >= 0 \
                      && hands[hands_index][0].size() == 2 && (!hands[hands_index][7] || (hands[hands_index][7] \
                      && double_allowed_after_split)) && action == 'D' || gambler_chips_cash - wager_total - \
                      hands[hands_index][9] < 0 && hands[hands_index][0].size() > 2 && action == 'H') {
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

def take_action() {
    if (action == 'H') {
        cut_card_drawn = deal_card(hands[hands_index][0])
    }
    else if (action == 'D') {
        hands[hands_index][9] = 2 * hands[hands_index][9]
        println "You have increased your wager to " + hands[hands_index][9]
        make_sound('Bottle.aiff')
        println()
        cut_card_drawn = deal_card(hands[hands_index][0])
    }
    else if (action == 'S') {
        println "You have increased your wager by " + wager
        make_sound('Bottle.aiff')
        println()
        split_card = hands[hands_index][0][1]
        hands[hands_index][0].remove(split_card)
        hands[hands.size()] = [[],0,[],false,false,false,false,false,false,wager]
        hands[hands.size() - 1][0].add(split_card)
        cut_card_drawn = deal_card(hands[hands_index][0])
        cut_card_drawn = deal_card(hands[hands.size() - 1][0])
    }
    get_hand_info()
    return cut_card_drawn
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
        wager_total += hands[i][9]
    }
    return wager_total
}

def results() {
    println()
    println 'Game over. '
    proceeds = 0.0
    for (int i = 1;i<hands.size();i++) {
        if (hands.size() > 2) {
            printf 'Hand ' + i +': '
        }
        if ((hands[0][1] > 21 && !hands[i][6]) || (hands[0][1] < hands[i][1]) && !hands[i][5] && !hands[i][6]) {
            hands_won += 1
            if (hands[i][8]) {
                doubled_hands_won += 1
            }
            printf 'Winner! :)'
            make_sound('Glass.aiff')
            if (hands[i][4]) {
                blackjack_won += 1
                gambler_chips_cash += (hands[i][9] * 1.5).round(2)
                proceeds += (hands[i][9] * 1.5).round(2)
            } else {
                gambler_chips_cash += hands[i][9]
                proceeds += hands[i][9]
            }
        }
        else if (hands[i][5]) {
            hands_surrender += 1
            printf 'Your wager has been split with the house.'
            gambler_chips_cash -= (hands[i][9] / 2).round(2)
            proceeds -= (hands[i][9] / 2).round(2)
        }
        else if (hands[0][1] > hands[i][1] || hands[i][6]) {
            hands_lost += 1
            if (hands[i][8]) {
                doubled_hands_lost += 1
            }
            printf 'The house wins. :('
            if (!hands[i][6]) {
                make_sound('Basso.aiff')
            }
            gambler_chips_cash -= hands[i][9]
            proceeds -= hands[i][9]
        }
        else if (hands[0][1] == hands[i][1]) {
            hands_push += 1
            if (hands[i][8]) {
                doubled_hands_push += 1
            }
            if (hands[i][4]) {
                blackjack_push += 1
            }
            printf 'Push. :|'
        }
        println()
        sleep(500)
    }
    cash = proceeds - proceeds.intValue()
    cash_string = '$' + cash.toString()
    if (cash > 0) {
        println 'Total proceeds: ' + "${(char)27}[32;40"+'m' + proceeds.intValue().toString() + default_style + \
          ' chips and ' + "${(char)27}[32;40"+'m' + cash_string + default_style
    } else {
        printf 'Total proceeds: '
        if (proceeds >= 0) {
            printf "${(char)27}[32;40"+'m' + proceeds.intValue().toString()
        } else {
            printf "${(char)27}[31;40"+'m' + proceeds.intValue().toString()
        }
        printf default_style
        println()
    }
    println()
    return gambler_chips_cash
}

def show_hand() {
    if (hands.size() > 2) {
        printf 'Your Hand ' + hands_index +': '
    } else {
        printf 'Your Hand: '
    }
    for (card in hands[hands_index][0]) {
        style = colorize(card[0])
        printf style+card[0]
        printf default_style+' '
    }
    printf '('
    if (hands[hands_index][3] && hands[hands_index][1] != 21) {
        printf 'soft '
    }
    printf hands[hands_index][1] + ')'
    printf '\n'
    if (hands.size() > 2) {
        println()
    }
}

def show_dealers_hand() {
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

def dealer_hits() {
    dealer_folds = true
    for (int i = 1;i<hands.size();i++) {
        if (!hands[i][4] && !hands[i][5] && !hands[i][6]) {
            dealer_folds = false
            break
        }
    }
    show_dealers_hand()
    if (!dealer_folds) {
        while (hands[0][1] < 17 || (!dealer_stands_soft17 && hands[0][1] == 17 && hands[0][3])) {
            sleep(1000)
            println 'Dealer hits.'
            cut_card_drawn = deal_card(hands[0][0])
            get_hand_info()
            show_dealers_hand()
        }
    }
    if (hands[0][1] > 21) {
        println 'Dealer busts!'
    }
    return cut_card_drawn
}

def reaction() {
    if (action == 'A') {
        if (hands[hands_index][0].size() == 2) {
            hands[hands_index][4] = true
            println 'Blackjack!'
        } else {
            println 'Your hand stands at 21.'
        }
        make_sound('Ping.aiff')
        sleep(1000)
    }
    else if (action == '?') {
        (correct_strategy, rule) = check_strategy()
        println rule
    } else {
        (correct_strategy, rule) = check_strategy()
        total_attempts += 1
        if (correct_strategy) {
            accurate_attempts += 1
            println 'Your strategy is correct.'
            println rule
        } else {
            println 'Your strategy is incorrect.'
            printf rule
            make_sound('Basso.aiff')
            println()
        }
        println()
        if (action == 'Q') {
            hands[hands_index][5] =true
            println 'You have surrendered your hand.'
            println()
        } else {
            if (action == 'S') {
                hands[hands_index][7] = true
                cut_card_drawn = take_action()
            } else if (action in ['H', 'D']) {
                cut_card_drawn = take_action()
            }
            if (hands[hands_index][1] > 21) {
                hands[hands_index][6] =true
                printf 'You bust!'
                make_sound('Sosumi.aiff')
                println()
            }
        }
        show_hand()
        if (action == 'D') {
            if (hands[hands_index][1] == 21) {
                make_sound('Ping.aiff')
            }
            sleep(1000)
        }
    }
    return cut_card_drawn
}

def show_outcome() {
    gambler_balance = gambler_chips_cash - gambler_account
    if (gambler_balance == 0) {
        println 'You leave with nothing. Play again if you dare!'
    }
    else if (gambler_balance > 0) {
        println 'Credit balance of ' + "${(char)27}[32;40"+'m' + \
          java.text.NumberFormat.currencyInstance.format(gambler_balance) + default_style + '. Cash dispensed below.'
    } else {
        println 'Balance due ' + "${(char)27}[31;40"+'m' + \
          java.text.NumberFormat.currencyInstance.format(-1 * gambler_balance) + default_style + \
          '. Insert credit card below.'
    }
    println()
}

def winloss(Integer num_won, Integer num_lost, Integer num_surrender, Integer num_push, Boolean doubled) {
    if (doubled) {
        printf 'Doubled '
    }
    if (surrender_allowed) {
        printf 'Win-Loss-Surrender-Push: '
    } else {
        printf 'Win-Loss-Push: '
    }
    if (num_won > 0) {
        printf "${(char)27}[32;40"+'m'
    }
    printf num_won.toString() + default_style + '-'
    if (num_lost > 0) {
        printf "${(char)27}[31;40"+'m'

    }
    printf  num_lost.toString() + default_style + '-'

    if (surrender_allowed) {
        printf num_surrender + '-'
    }
    println num_push
}

def blackjack_stat() {
    blackjacks = blackjack_won + blackjack_push
    printf 'Blackjacks: '
    printf blackjacks.toString()
    if (blackjacks > 0) {
        printf ' ('
        if (blackjack_won > 0) {
            printf "${(char) 27}[32;40" + 'm'
        }
        println blackjack_won + default_style + ' won)'
    } else {
        println()
    }
}

def end_of_game() {
    credit_avail = credit_limit - gambler_account
    cashier_input_err = true
    while (cashier_input_err) {
        cont_options = []
        valid_cont_actions = []
        if (gambler_chips_cash.intValue() >= min_wager) {
            cont_options.add('Press <Enter> to play again or enter Q to quit')
            valid_cont_actions.add('')
            valid_cont_actions.add('Q')
            if (credit_avail >= (min_wager - gambler_chips_cash.intValue())) {
                cont_options.add('or C to go to the cashier')
                valid_cont_actions.add('C')
            }
        }
        else if (credit_avail >= (min_wager - gambler_chips_cash.intValue())) {
            cont_options.add('Enter Q to quit or C to go to the cashier')
            valid_cont_actions.add('Q')
            valid_cont_actions.add('C')
        }
        if (cont_options.size() > 0) {
            cont_prompt = ''
            if (cont_options.size() > 1) {
                for (i = 0; i < (cont_options.size() - 1); i++) {
                    cont_prompt += cont_options[i] + ' '
                }
                cont_prompt += cont_options[cont_options.size() - 1] + ': '
            } else {
                cont_prompt = cont_options[0] + ': '
            }
            cont_action = System.console().readLine cont_prompt
            cont_action = cont_action.trim().toUpperCase()
            if (cont_action.isEmpty() && valid_cont_actions.contains('')) {
                cashier_input_err = false
                play_again = true
                clear_screen()
            } else if (cont_action == 'Q' && valid_cont_actions.contains('Q')) {
                cashier_input_err = false
                play_again = false
                println()
                show_outcome()
            } else if (cont_action == 'C' && valid_cont_actions.contains('C')) {
                (gambler_account, gambler_chips_cash) = cashier('E')
                credit_avail = credit_limit - gambler_account
                standings()
                if (gambler_chips_cash.intValue() == 0 && credit_avail == 0) {
                    cashier_input_err = false
                    play_again = false
                    println()
                    show_outcome()
                } else {
                    play_again = true
                }
            } else {
                printf 'Try again. '
                make_sound('Hero.aiff')
                println()
            }
        } else {
            cashier_input_err = false
            play_again = false
            show_outcome()
        }
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

def make_sound(sound_name) {
    command = "afplay /System/Library/Sounds/" + sound_name
    if (sounds) {
        command.execute()
    }
}

def mainMethod() {
    (default_style, gambler_account, gambler_chips_cash, num_decks, dealer_stands_soft17, double_allowed_after_split, \
      surrender_allowed, total_attempts, accurate_attempts, hands_won, hands_push, hands_lost, hands_surrender, \
      doubled_hands_won, doubled_hands_push, doubled_hands_lost, doubled_hands_surrender, blackjack_won, \
      blackjack_push) = init_game()
    clear_screen()
    (shoe, cut_card_drawn) = init_shoe()
    play_again = true

    while (play_again) {
        wager = init_wager()
        hands_index = 1
        if (cut_card_drawn) {
            (shoe, cut_card_drawn) = init_shoe()
        }
        hands_played = deal()
        show_start()
        while (hands_index < hands.size()) {
            action = get_action()
            cut_card_drawn = reaction()
            while (action !in ['','A','D','Q'] && !hands[hands_index][6]) {
                action = get_action()
                cut_card_drawn = reaction()
            }
            if (action == 'S') {
                show_hand()
            }
            hands_played += 1
            hands_index += 1
            if (hands.size() > hands_index) {
                show_hand()
            }
        }
        cut_card_drawn = dealer_hits()
        gambler_chips_cash = results()
        cash = gambler_chips_cash - gambler_chips_cash.intValue()
        cash_string = '$' + cash.toString()
        printf 'Result: ' + "${(char)27}[32;40"+'m' + gambler_chips_cash.intValue().toString() + default_style + \
          ' chips'
        if (cash > 0) {
             printf ' and ' + "${(char)27}[32;40"+'m' + cash_string + default_style
        }
        println()
        println()
        if (display_winloss_stat) {
            winloss(hands_won, hands_lost, hands_surrender, hands_push, false)
            if (!display_doubled_winloss_stat && !display_blackjack_stat) {
                println()
            }
        }
        if (display_doubled_winloss_stat) {
            winloss(doubled_hands_won, doubled_hands_lost, doubled_hands_surrender, doubled_hands_push, true)
            if (!display_blackjack_stat) {
                println()
            }
        }
        if (display_blackjack_stat) {
            blackjack_stat()
            println()
        }
        if (complete_accuracy && accurate_attempts != total_attempts) {
            make_sound('Funk.aiff')
            println 'Your accuracy has fallen below 100%.'
            play_again = false
            show_outcome()
        } else {
            play_again = end_of_game()
        }
    }
}

mainMethod()
