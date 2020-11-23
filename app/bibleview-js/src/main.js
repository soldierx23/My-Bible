/*
 * Copyright (c) 2020 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
 *
 * This file is part of And Bible (http://github.com/AndBible/and-bible).
 *
 * And Bible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * And Bible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with And Bible.
 * If not, see http://www.gnu.org/licenses/.
 */

import { createApp } from 'vue'

// We will inject here callbacks / stuff that is manipulated by Android Javascript interface
window.bibleView = {};
window.bibleViewDebug = {};

const origConsole = window.console;

// Override normal console, so that argument values also propagate to Android logcat
const myConsole = {
    _msg(s, args) {
        return `${s} ${args}`
    },
    log(s, ...args) {
        origConsole.log(this._msg(s, args))
    },
    error(s, ...args) {
        origConsole.error(this._msg(s, args))
    },
    warn(s, ...args) {
        origConsole.warn(this._msg(s, args))
    }
}

window.console = myConsole;

import BibleView from "@/components/BibleView";
const app = createApp(BibleView);
app.mount('#app')

