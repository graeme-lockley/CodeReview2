var loggedOnUser = undefined;

function applyTemplate(templateName, templateState) {
    var theTemplate = $("#" + templateName).html();
    return _.template(theTemplate, templateState);
}

function calculateElementID(element) {
    if (element == undefined) {
        return undefined;
    } else {
        var elementID = element.getAttribute("id");
        if (elementID == null) {
            return calculateElementID(element.parentNode);
        } else {
            return elementID;
        }
    }
}

//////////////////////

function Feedback(feedback) {
    this.feedback = feedback || {};
    this.feedbackOnName = {};
}
Feedback.prototype.addFeedbackItem = function (feedbackItem) {
    var commentaryItemsForLine = this.feedback[feedbackItem.lineNumber];
    if (commentaryItemsForLine == undefined) {
        commentaryItemsForLine = [];
        this.feedback[feedbackItem.lineNumber] = commentaryItemsForLine;
    }
    commentaryItemsForLine.push(feedbackItem);
    this.addOnName(feedbackItem.name(), feedbackItem);
};
Feedback.prototype.addCommentDraft = function (commentaryDraft) {
    this.addFeedbackItem(commentaryDraft);
    events.event("PostADD-ItemDraft")(commentaryDraft);
};
Feedback.prototype.addIssueDraft = function (issueDraft) {
    this.addFeedbackItem(issueDraft);
    events.event("PostADD-ItemDraft")(issueDraft);
};
Feedback.prototype.findOnName = function (name) {
    return this.feedbackOnName[name];
};
Feedback.prototype.addOnName = function (name, item) {
    this.feedbackOnName[name] = item;
};
Feedback.prototype.removeOnName = function (name) {
    for (var lineNumber in this.feedback) {
        if (this.feedback.hasOwnProperty(lineNumber)) {
            var feedbackOnLine = this.feedback[lineNumber];

            for (var i = 0; i < feedbackOnLine.length; i += 1) {
                if (feedbackOnLine[i].name() === name) {
                    feedbackOnLine.splice(i, 1);
                    var result = this.feedbackOnName[name];
                    delete this.feedbackOnName[name];
                    return result;
                }
            }
        }
    }
    return undefined;
};
Feedback.prototype.removeFeedbackDraft = function (feedbackDraft) {
    this.removeOnName(feedbackDraft.name());
    events.event("PostREMOVE-FeedbackDraft")(feedbackDraft);
};
Feedback.prototype.commitCommentDraft = function (commentaryDraft, revisionEntryID, comment) {
    events.event("Start-CommitCommentDraft")(commentaryDraft);
    commentaryDraft.commitDraft(revisionEntryID, comment, function (dbid, when) {
        feedback.removeOnName(commentaryDraft.name());
        var feedbackComment = new FeedbackComment(commentaryDraft.lineNumber, dbid, comment, loggedOnUser.name, when, []);
        feedback.addFeedbackItem(feedbackComment);
        events.event("Success-CommitCommentDraft")(commentaryDraft, feedbackComment);
    }, function (reason) {
        events.event("Failure-CommitCommentDraft")(commentaryDraft, reason);
    });
};
Feedback.prototype.commitIssueDraft = function (issueDraft, revisionEntryID, comment) {
    events.event("Start-CommitIssueDraft")(issueDraft);
    issueDraft.commitDraft(revisionEntryID, comment, function (dbid, when) {
        feedback.removeOnName(issueDraft.name());
        var issue = new Issue(issueDraft.lineNumber, dbid, comment, loggedOnUser.name, when, [], "Open");
        feedback.addFeedbackItem(issue);
        events.event("Success-CommitIssueDraft")(issueDraft, issue);
    }, function (reason) {
        events.event("Failure-CommitIssueDraft")(issueDraft, reason);
    });
};
Feedback.prototype.removeDraftFeedbackOnComment = function (draftFeedbackOnComment) {
    this.removeOnName(draftFeedbackOnComment.name());
    draftFeedbackOnComment.clearDraft();
    events.event("PostREMOVE-FeedbackOnCommentFeedback")(draftFeedbackOnComment);
};
Feedback.prototype.commitDraftFeedbackOnComment = function (draftFeedbackOnComment, comment) {
    events.event("Start-CommitDraftFeedbackOnComment")(draftFeedbackOnComment);
    draftFeedbackOnComment.commitDraft(comment, function (dbid, when) {
        feedback.removeOnName(draftFeedbackOnComment.name());
        draftFeedbackOnComment.clearDraft();
        var feedbackOnComment = new FeedbackOnComment(dbid, comment, loggedOnUser.name, when);
        draftFeedbackOnComment.comment.addFeedbackOnComment(feedbackOnComment);
        events.event("Success-CommitDraftFeedbackOnComment")(draftFeedbackOnComment, feedbackOnComment);
    }, function (reason) {
        events.event("Failure-CommitDraftFeedbackOnComment")(draftFeedbackOnComment, reason);
    });
};
Feedback.prototype.removeDraftFeedbackOnIssue = function (draftFeedbackOnIssue) {
    this.removeOnName(draftFeedbackOnIssue.name());
    draftFeedbackOnIssue.clearDraft();
    events.event("PostREMOVE-FeedbackOnIssue")(draftFeedbackOnIssue);
};
Feedback.prototype.commitDraftFeedbackOnIssue = function (draftFeedbackOnIssue, comment) {
    events.event("Start-CommitDraftFeedbackOnIssue")(draftFeedbackOnIssue);
    draftFeedbackOnIssue.commitDraft(comment, function (dbid, when) {
        feedback.removeOnName(draftFeedbackOnIssue.name());
        draftFeedbackOnIssue.clearDraft();
        var feedbackOnIssue = new FeedbackOnIssue(dbid, comment, loggedOnUser.name, when);
        draftFeedbackOnIssue.issue.addFeedbackOnIssue(feedbackOnIssue);
        events.event("Success-CommitDraftFeedbackOnIssue")(draftFeedbackOnIssue, feedbackOnIssue);
    }, function (reason) {
        events.event("Failure-CommitDraftFeedbackOnIssue")(draftFeedbackOnIssue, reason);
    });
};
Feedback.prototype.dumpModel = function () {
    for (var lineNumber in this.feedback) {
        if (this.feedback.hasOwnProperty(lineNumber)) {
            console.info("Line: " + lineNumber);
            var feedbackOnLine = this.feedback[lineNumber];

            for (var i = 0; i < feedbackOnLine.length; i += 1) {
                console.info("  " + feedbackOnLine[i].name());
            }
        }
    }
};
Feedback.prototype.mustAnnotate = function (lineNumber) {
    var feedbackOnLine = this.feedback[lineNumber] || [];

    for (var i = 0; i < feedbackOnLine.length; i += 1) {
        if (feedbackOnLine[i].mustAnnotate()) {
            return true;
        }
    }
    return false;
};

//--------------

function Events(events) {
    this.events = events;
}
Events.prototype.event = function (eventName) {
    if (this.events.hasOwnProperty(eventName)) {
        console.info("Found event handler [" + eventName + "]");
        return this.events[eventName];
    } else {
        console.error("No event handler [" + eventName + "] found");
        return function () {
        };
    }
};
Events.prototype.registerEvent = function (eventName, eventHandler) {
    this.events[eventName] = eventHandler;
};

//--------------

function FeedbackCommentDraft(lineNumber) {
    this.type = "commentarydraft";
    this.lineNumber = lineNumber;
    this.notionalID = newUniqueID();
}
FeedbackCommentDraft.prototype.name = function () {
    return "FCD" + this.notionalID;
};
FeedbackCommentDraft.prototype.mustAnnotate = function () {
    return false;
};
FeedbackCommentDraft.prototype.commitDraft = function (revisionEntryID, comment, successContinuation, failureContinuation) {
    var content = JSON.stringify({"comment": comment, "authorID": loggedOnUser.id, "revisionEntryID": revisionEntryID, "lineNumber": this.lineNumber, "status": "closed"});

    console.log(content);

    $.ajax({
        type: "PUT",
        url: "/feedback",
        data: content,
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            successContinuation(data.id, data.date);
        },
        error: function (jqXHR, status) {
            failureContinuation(status);
        }
    });
};

//--------------

function IssueDraft(lineNumber) {
    this.type = "issuedraft";
    this.lineNumber = lineNumber;
    this.notionalID = newUniqueID();
}
IssueDraft.prototype.name = function () {
    return "FID" + this.notionalID;
};
IssueDraft.prototype.mustAnnotate = function () {
    return false;
};
IssueDraft.prototype.commitDraft = function (revisionEntryID, comment, successContinuation, failureContinuation) {
    var content = JSON.stringify({"comment": comment, "authorID": loggedOnUser.id, "revisionEntryID": revisionEntryID, "lineNumber": this.lineNumber, "status": "open"});

    console.log(content);

    $.ajax({
        type: "PUT",
        url: "/feedback",
        data: content,
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            successContinuation(data.id, data.when);
        },
        error: function (jqXHR, status) {
            failureContinuation(status);
        }
    });
};

//--------------

function FeedbackComment(lineNumber, dbid, commentary, author, when, feedbackOnComment) {
    this.type = "commentary";
    this.lineNumber = lineNumber;
    this.dbid = dbid;
    this.commentary = commentary;
    this.author = author;
    this.when = when;
    this.feedbackOnComment = feedbackOnComment;
    this.draftFeedbackOnComment = undefined;
}
FeedbackComment.prototype.name = function () {
    return "DBID" + this.dbid;
};
FeedbackComment.prototype.mustAnnotate = function () {
    return true;
};
FeedbackComment.prototype.addFeedbackOnComment = function (feedback) {
    this.feedbackOnComment.push(feedback);
};
FeedbackComment.prototype.createDraftFeedbackOnComment = function () {
    var draftFeedbackOnComment = new DraftFeedbackOnComment(this);
    this.draftFeedbackOnComment = draftFeedbackOnComment;
    feedback.addOnName(draftFeedbackOnComment.name(), draftFeedbackOnComment);
    events.event("PostADD-DraftFeedbackOnItem")(draftFeedbackOnComment);
};
FeedbackComment.prototype.clearDraft = function () {
    this.draftFeedbackOnComment = undefined;
};
FeedbackComment.prototype.refreshFromJSON = function (data) {
    this.lineNumber = data.lineNumber;
    this.commentary = data.comment;
    this.dbid = data.id;
    this.author = data.author.name;
    this.when = data.date;
    this.draftFeedbackOnComment = undefined;
    this.feedbackOnComment = [];

    for (var i = 0; i < data.responses.length; i += 1) {
        var response = data.responses[i];
        this.addFeedbackOnComment(new FeedbackOnComment(response.id, response.comment, response.author.name, response.date));
    }
};

//--------------

function Issue(lineNumber, dbid, commentary, author, when, feedbackOnIssue, status) {
    this.type = "issue";
    this.lineNumber = lineNumber;
    this.dbid = dbid;
    this.commentary = commentary;
    this.author = author;
    this.when = when;
    this.feedbackOnIssue = feedbackOnIssue;
    this.draftFeedbackOnIssue = undefined;
    this.status = status;
}
Issue.prototype.name = function () {
    return "DBID" + this.dbid;
};
Issue.prototype.mustAnnotate = function () {
    return true;
};
Issue.prototype.addFeedbackOnIssue = function (feedback) {
    this.feedbackOnIssue.push(feedback);
};
Issue.prototype.createDraftFeedbackOnIssue = function () {
    var draftFeedbackOnIssue = new DraftFeedbackOnIssue(this);
    this.draftFeedbackOnIssue = draftFeedbackOnIssue;
    feedback.addOnName(draftFeedbackOnIssue.name(), draftFeedbackOnIssue);
    events.event("PostADD-DraftFeedbackOnItem")(draftFeedbackOnIssue);
};
Issue.prototype.clearDraft = function () {
    this.draftFeedbackOnIssue = undefined;
};
Issue.prototype.closeIssue = function () {
    events.event("Start-CloseIssue")(this);
    var issue = this;
    $.ajax({
        type: "PUT",
        url: "/feedback/" + issue.dbid + "/close/" + loggedOnUser.id,
        data: "{}",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            issue.status = "Closed";
            events.event("Success-CloseIssue")(issue);
        },
        error: function (jqXHR, status) {
            events.event("Failure-CloseIssue")(issue);
        }
    });
};
Issue.prototype.refresh = function () {
    events.event("Start-RefreshIssue")(this);
    var issue = this;
    $.ajax({
        type: "GET",
        url: "/feedback/" + issue.dbid,
        data: "{}",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            issue.refreshFromJSON(data);
            events.event("Success-RefreshIssue")(issue);
        },
        error: function (jqXHR, status) {
            events.event("Failure-RefreshIssue")(issue);
        }
    });
};
Issue.prototype.refreshFromJSON = function (data) {
    this.lineNumber = data.lineNumber;
    this.commentary = data.comment;
    this.dbid = data.id;
    this.author = data.author.name;
    this.when = data.date;
    this.status = data.status;
    this.draftFeedbackOnIssue = undefined;
    this.feedbackOnIssue = [];

    for (var i = 0; i < data.responses.length; i += 1) {
        var response = data.responses[i];
        this.addFeedbackOnIssue(new FeedbackOnIssue(response.id, response.comment, response.author.name, response.date));
    }
};

//--------------

function FeedbackOnComment(dbid, commentary, author, when) {
    this.dbid = dbid;
    this.commentary = commentary;
    this.author = author;
    this.when = when;
}
FeedbackOnComment.prototype.name = function () {
    return "DBID" + this.dbid;
};

//--------------

function FeedbackOnIssue(dbid, commentary, author, when) {
    this.dbid = dbid;
    this.commentary = commentary;
    this.author = author;
    this.when = when;
}
FeedbackOnIssue.prototype.name = function () {
    return "DBID" + this.dbid;
};

//--------------

function DraftFeedbackOnComment(comment) {
    this.dbid = newUniqueID();
    this.commentary = "";
    this.comment = comment;
}
DraftFeedbackOnComment.prototype.name = function () {
    return "DFCF" + this.dbid;
};
DraftFeedbackOnComment.prototype.item = function () {
    return this.comment;
};
DraftFeedbackOnComment.prototype.commitDraft = function (comment, successContinuation, failureContinuation) {
    var content = JSON.stringify({"comment": comment, "authorID": loggedOnUser.id, "feedbackID": this.item().dbid});

    console.log(content);

    $.ajax({
        type: "PUT",
        url: "/responses",
        data: content,
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            successContinuation(data.id, data.when);
        },
        error: function (jqXHR, status) {
            failureContinuation(status);
        }
    });
};
DraftFeedbackOnComment.prototype.clearDraft = function () {
    this.comment.clearDraft();
};

//--------------

function DraftFeedbackOnIssue(issue) {
    this.dbid = newUniqueID();
    this.commentary = "";
    this.issue = issue;
}
DraftFeedbackOnIssue.prototype.name = function () {
    return "DFIF" + this.dbid;
};
DraftFeedbackOnIssue.prototype.item = function () {
    return this.issue;
};
DraftFeedbackOnIssue.prototype.commitDraft = function (comment, successContinuation, failureContinuation) {
    var content = JSON.stringify({"comment": comment, "authorID": loggedOnUser.id, "feedbackID": this.item().dbid});

    console.log(content);

    $.ajax({
        type: "PUT",
        url: "/responses",
        data: content,
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            successContinuation(data.id, data.when);
        },
        error: function (jqXHR, status) {
            failureContinuation(status);
        }
    });
};
DraftFeedbackOnIssue.prototype.clearDraft = function () {
    this.issue.clearDraft();
};

//--------------
function UICommentLine(lineNumber) {
    this.commentLine = lineNumber instanceof CommentLine ? lineNumber : new CommentLine(lineNumber);
}
UICommentLine.prototype.$atLastOfFeedback = function () {
    return $("#" + this.commentLine.name() + " td:last div:first");
};
UICommentLine.prototype.appendFeedback = function (commentary) {
    this.$atLastOfFeedback().append(applyTemplate("show_" + commentary.type, {feedbackItem: commentary}));
    if (feedback.mustAnnotate(commentary.lineNumber)) {
        annotateLine(commentary.lineNumber);
    }
};
UICommentLine.prototype.removeFeedbackDraft = function (feedbackDraft) {
    $("#" + feedbackDraft.name()).remove();
};
//--------------

var feedback = new Feedback();
var events = new Events({});

events.registerEvent("PostADD-FeedbackOnComment", function (feedbackComment, feedback) {
    $("#" + feedbackComment.name()).html(applyTemplate("show_" + feedbackComment.type, {feedbackItem: feedbackComment}));
    registerButtonHandler();
});
events.registerEvent("PostADD-ItemDraft", function (itemDraft) {
    var uiCommentLine = new UICommentLine(itemDraft.lineNumber);
    uiCommentLine.appendFeedback(itemDraft);
    registerButtonHandler();
});
events.registerEvent("PostADD-DraftFeedbackOnItem", function (draftFeedbackOnItem) {
    var item = draftFeedbackOnItem.item();
    $("#" + item.name()).html(applyTemplate("show_" + item.type, {feedbackItem: item}));
    registerButtonHandler();
});
events.registerEvent("Click-CancelDraftFeedbackOnCommentBTN", function (event) {
    var draftFeedbackOnComment = feedback.findOnName(calculateElementID(event.target));
    feedback.removeDraftFeedbackOnComment(draftFeedbackOnComment);
});
events.registerEvent("Click-SaveDraftFeedbackOnCommentBTN", function (event) {
    var draftFeedbackOnComment = feedback.findOnName(calculateElementID(event.target));
    feedback.commitDraftFeedbackOnComment(draftFeedbackOnComment, $("#" + draftFeedbackOnComment.name() + "TXT").val());
});
events.registerEvent("Click-CancelDraftFeedbackOnIssueBTN", function (event) {
    var draftFeedbackOnIssue = feedback.findOnName(calculateElementID(event.target));
    feedback.removeDraftFeedbackOnIssue(draftFeedbackOnIssue);
});
events.registerEvent("Click-SaveDraftFeedbackOnIssueBTN", function (event) {
    var draftFeedbackOnIssue = feedback.findOnName(calculateElementID(event.target));
    feedback.commitDraftFeedbackOnIssue(draftFeedbackOnIssue, $("#" + draftFeedbackOnIssue.name() + "TXT").val());
});
events.registerEvent("Start-CommitDraftFeedbackOnComment", function (commentaryDraft) {
    $("#" + commentaryDraft.name() + " button[name='SaveDraftFeedbackOnIssueBTN']").html("saving");
    $("#" + commentaryDraft.name() + " button").attr("disabled", "disabled");
});
events.registerEvent("Success-CommitDraftFeedbackOnComment", function (draftFeedbackOnComment, feedbackOnComment) {
    var feedbackComment = draftFeedbackOnComment.item();
    $("#" + feedbackComment.name()).html(applyTemplate("show_" + feedbackComment.type, {feedbackItem: feedbackComment}));
    registerButtonHandler();
});
events.registerEvent("Failure-CommitDraftFeedbackOnComment", function (draftFeedbackOnComment, reason) {
    alert("Unable to save new response on comment: " + reason.toString());
    $("#" + draftFeedbackOnComment.name() + " button[name='SaveDraftFeedbackOnIssueBTN']").html("<span class=\"glyphicon glyphicon-comment\"></span> Save");
    $("#" + draftFeedbackOnComment.name() + " button").removeAttr("disabled");
});
events.registerEvent("Start-CommitDraftFeedbackOnIssue", function (draftFeedbackOnIssue) {
    $("#" + draftFeedbackOnIssue.name() + " button[name='SaveDraftFeedbackOnIssueBTN']").html("saving");
    $("#" + draftFeedbackOnIssue.name() + " button").attr("disabled", "disabled");
});
events.registerEvent("Success-CommitDraftFeedbackOnIssue", function (draftFeedbackOnIssue, feedbackOnComment) {
    var issue = draftFeedbackOnIssue.item();
    $("#" + issue.name()).html(applyTemplate("show_" + issue.type, {feedbackItem: issue}));
    registerButtonHandler();
});
events.registerEvent("Failure-CommitDraftFeedbackOnIssue", function (draftFeedbackOnIssue, reason) {
    alert("Unable to save response on issue: " + reason.toString());
    $("#" + draftFeedbackOnIssue.name() + " button[name='SaveDraftFeedbackOnIssueBTN']").html("<span class=\"glyphicon glyphicon-comment\"></span> Save");
    $("#" + draftFeedbackOnIssue.name() + " button").removeAttr("disabled");
});
events.registerEvent("PostREMOVE-FeedbackOnCommentFeedback", function (draftFeedbackOnComment) {
    var comment = draftFeedbackOnComment.item();
    $("#" + comment.name()).html(applyTemplate("show_" + comment.type, {feedbackItem: comment}));
    registerButtonHandler();
});
events.registerEvent("PostREMOVE-FeedbackOnIssue", function (draftFeedbackOnIssue) {
    var issue = draftFeedbackOnIssue.item();
    $("#" + issue.name()).html(applyTemplate("show_" + issue.type, {feedbackItem: issue}));
    registerButtonHandler();
});
events.registerEvent("PostREMOVE-FeedbackDraft", function (feedbackDraft) {
    var uiCommentLine = new UICommentLine(feedbackDraft.lineNumber);
    uiCommentLine.removeFeedbackDraft(feedbackDraft);
    registerButtonHandler();
});
events.registerEvent("Click-SaveNewCommentBTN", function (event) {
    var feedbackCommentDraft = feedback.findOnName(calculateElementID(event.target));
    feedback.commitCommentDraft(feedbackCommentDraft, parseInt($("#revisionNumber").html()), $("#" + feedbackCommentDraft.name() + "TXT").val());
});
events.registerEvent("Click-CancelNewCommentBTN", function (event) {
    var feedbackCommentDraft = feedback.findOnName(calculateElementID(event.target));
    feedback.removeFeedbackDraft(feedbackCommentDraft);
    registerButtonHandler();
});
events.registerEvent("Click-SaveNewIssueBTN", function (event) {
    var issueDraft = feedback.findOnName(calculateElementID(event.target));
    feedback.commitIssueDraft(issueDraft, parseInt($("#revisionNumber").html()), $("#" + issueDraft.name() + "TXT").val());
});
events.registerEvent("Click-CancelNewIssueBTN", function (event) {
    var issueDraft = feedback.findOnName(calculateElementID(event.target));
    feedback.removeFeedbackDraft(issueDraft);
    registerButtonHandler();
});
events.registerEvent("Start-CommitCommentDraft", function (commentaryDraft) {
    $("#" + commentaryDraft.name() + " button[name='SaveNewCommentBTN']").html("saving");
    $("#" + commentaryDraft.name() + " button").attr("disabled", "disabled");
});
events.registerEvent("Success-CommitCommentDraft", function (commentaryDraft, feedbackComment) {
    var uiCommentLine = new UICommentLine(commentaryDraft.lineNumber);
    uiCommentLine.removeFeedbackDraft(commentaryDraft);
    uiCommentLine.appendFeedback(feedbackComment);
    registerButtonHandler();
});
events.registerEvent("Failure-CommitCommentDraft", function (commentaryDraft, reason) {
    alert("Unable to save new comment: " + reason.toString());
    $("#" + commentaryDraft.name() + " button[name='SaveNewCommentBTN']").html("<span class=\"glyphicon glyphicon-comment\"></span> Save");
    $("#" + commentaryDraft.name() + " button").removeAttr("disabled");
});
events.registerEvent("Start-CommitIssueDraft", function (issueDraft) {
    $("#" + issueDraft.name() + " button[name='SaveNewIssueBTN']").html("saving");
    $("#" + issueDraft.name() + " button").attr("disabled", "disabled");
});
events.registerEvent("Success-CommitIssueDraft", function (issueDraft, issue) {
    var uiCommentLine = new UICommentLine(issueDraft.lineNumber);
    uiCommentLine.removeFeedbackDraft(issueDraft);
    uiCommentLine.appendFeedback(issue);
    registerButtonHandler();
});
events.registerEvent("Failure-CommitIssueDraft", function (issueDraft, reason) {
    alert("Unable to save new issue: " + reason.toString());
    $("#" + issueDraft.name() + " button[name='SaveNewIssueBTN']").html("<span class=\"glyphicon glyphicon-comment\"></span> Save");
    $("#" + issueDraft.name() + " button").removeAttr("disabled");
});
events.registerEvent("Click-AddCommentBTN", function (event) {
    var lineNumber = parseInt(calculateElementID(event.target).substr(8));
    var feedbackCommentDraft = new FeedbackCommentDraft(lineNumber);
    feedback.addCommentDraft(feedbackCommentDraft);
    registerButtonHandler();
});
events.registerEvent("Click-RaiseIssueBTN", function (event) {
    var lineNumber = parseInt(calculateElementID(event.target).substr(8));
    var issueDraft = new IssueDraft(lineNumber);
    feedback.addIssueDraft(issueDraft);
    registerButtonHandler();
});
events.registerEvent("Click-RespondToCommentBTN", function (event) {
    var feedbackComment = feedback.findOnName(calculateElementID(event.target));
    feedbackComment.createDraftFeedbackOnComment();
});
events.registerEvent("Click-RespondToIssueBTN", function (event) {
    var issue = feedback.findOnName(calculateElementID(event.target));
    issue.createDraftFeedbackOnIssue();
});
events.registerEvent("Click-CloseIssueBTN", function (event) {
    var issue = feedback.findOnName(calculateElementID(event.target));
    issue.closeIssue();
});
events.registerEvent("Start-CloseIssue", function (issue) {
});
events.registerEvent("Success-CloseIssue", function (issue) {
    issue.refresh();
});
events.registerEvent("Failure-CloseIssue", function (issue) {
});
events.registerEvent("Success-RefreshIssue", function (issue) {
    $("#" + issue.name()).html(applyTemplate("show_" + issue.type, {feedbackItem: issue}));
    registerButtonHandler();
});

events.registerEvent("OpenLine", function (lineNumber) {
    var commentLine = new CommentLine(lineNumber);
    $("#comment-" + lineNumber).remove();
    $("#" + lineNumber).parent().after(applyTemplate("initial_annotation", {commentLine: commentLine}));

    var feedbackItems = feedback.feedback[lineNumber];
    if (feedbackItems != undefined) {
        for (var lp = 0; lp < feedbackItems.length; lp += 1) {
            var feedbackItem = feedbackItems[lp];
            $("#" + commentLine.name() + " td:last div:first").append(applyTemplate("show_" + feedbackItem.type, {feedbackItem: feedbackItem}));
        }
    }
    $("#comment-" + lineNumber).collapse('toggle');

    registerButtonHandler();
});


//--------------

function newUniqueID() {
    while (true) {
        var proposedID = -Math.floor(Math.random() * 100000000000001);
        if ($("#" + proposedID).length == 0) {
            return proposedID;
        }
    }
}

function registerButtonHandler() {
    var addCommentButtons = $("button");
    addCommentButtons.off('click');
    addCommentButtons.click(function (event) {
        var buttonName = $(this).attr("name");
        events.event("Click-" + buttonName)(event);
    });
}

function annotateLine(lineNumber) {
    $("#" + lineNumber).prev().css("background-color", "yellow");
}

function annotateLines() {
    for (var lineNumber in feedback.feedback) {
        if (feedback.feedback.hasOwnProperty(lineNumber)) {
            annotateLine(lineNumber);
        }
    }
}

function openLine(lineNumber) {
    events.event("OpenLine")(lineNumber);
}

///////////////////////


function isAString(variable) {
    return (typeof variable == 'string' || variable instanceof String)
}

function CommentLine(lineNumber) {
    this.lineNumber = (isAString(lineNumber) ? parseInt(lineNumber.substring(8)) : lineNumber);
}

CommentLine.prototype.name = function () {
    return "comment-" + this.lineNumber;
};

CommentLine.prototype.toString = function () {
    return "[CommentLine: " + this.lineNumber + "]";
};

function CommentLineEntry(commentLine, sequenceNumber) {
    this.commentLine = commentLine;
    this.sequenceNumber = sequenceNumber;
}

CommentLineEntry.prototype.name = function () {
    return "comment-" + this.commentLine.lineNumber + "-" + this.sequenceNumber;
};

CommentLineEntry.prototype.textName = function () {
    return "comment-" + this.commentLine.lineNumber + "-" + this.sequenceNumber + "TA";
};

CommentLineEntry.prototype.textContent = function () {
    return $("#" + this.textName()).val();
};

CommentLineEntry.prototype.removeFromPage = function () {
    $("#" + this.name()).remove();
};

CommentLineEntry.prototype.save = function () {
    var content = JSON.stringify({"comment": this.textContent(), "authorID": loggedOnUser.id, "revisionEntryID": parseInt($("#revisionNumber").html()), "lineNumber": this.commentLine.lineNumber});
    var commentLineEntry = this;

    $.ajax({
        type: "PUT",
        url: "/feedback",
        data: content,
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            var commentLineComment = new CommentLineComment(commentLineEntry.commentLine, commentLineEntry.sequenceNumber, 123, commentLineEntry.textContent(), loggedOnUser.name, data.date);
            commentLineComment.replaceCommentLineEntry(commentLineEntry);
        },

        error: function (jqXHR, status) {
            alert("Oh dear... something went wrong...");
            alert(jqXHR.toString());
            alert(status);
        }
    })
};

CommentLineEntry.prototype.toString = function () {
    return "[CommentLineEntry: " + this.commentLine.toString() + ", " + this.sequenceNumber + "]";
};

function commentLineEntryFromString(str) {
    var dropHead = str.substr(8);
    var indexOfSlash = dropHead.indexOf("-");

    return new CommentLineEntry(new CommentLine(parseInt(dropHead.substr(0, indexOfSlash))), parseInt(dropHead.substr(indexOfSlash + 1)));
}

function CommentLineComment(commentLine, sequenceNumber, dbid, commentary, author, when) {
    this.commentLine = commentLine;
    this.sequenceNumber = sequenceNumber;
    this.dbid = dbid;
    this.commentary = commentary;
    this.author = author;
    this.when = when;
}

CommentLineComment.prototype.name = function () {
    return "comment-" + this.commentLine.lineNumber + "-" + this.sequenceNumber;
};

CommentLineComment.prototype.replaceCommentLineEntry = function (commentLineEntry) {
    $("#" + commentLineEntry.name()).html(applyTemplate("show_commentary", {commentLineComment: this}));
};

function commentLineItemName(lineNumber, lineCommentNumber) {
    return "comment-" + lineNumber + "-" + lineCommentNumber;
}

function numberOfCommentsAgainstLine(commentLine) {
    return $("#" + commentLine.name() + " td:last div ul").length;
}

function hasCommentLineOnPage(commentLine) {
    return $("#" + commentLine.name()).length > 0
}

function fetchLoggedOnUser() {
    $.ajax({
        type: "GET",
        url: "/auth/user",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            loggedOnUser = data;
        },
        error: function (jqXHR, status) {
            console.error(jqXHR);
        }
    });
}

$(document).ready(function () {
    function stuffOnLine(lineNumber) {
        var commentLine = new CommentLine(parseInt(event.target.id));

        if (!hasCommentLineOnPage(commentLine)) {
            openLine(commentLine.lineNumber);
        }

        $("#" + commentLine.name()).collapse('toggle');
    }

    $(".source").click(function (event) {
        stuffOnLine(parseInt(event.target.id));
    });
    $(function () {
        $("bob2").tablesorter();
    });

    fetchLoggedOnUser();

    $.ajax({
        type: "GET",
        url: "/revisionEntries/" + parseInt($("#revisionNumber").html()),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            for (var i = 0; i < data.feedback.length; i += 1) {
                var feedbackItem = data.feedback[i];

                if (feedbackItem.type == "issue") {
                    var issue = new Issue();
                    issue.refreshFromJSON(feedbackItem);
                    feedback.addFeedbackItem(issue);
                } else if (feedbackItem.type == "comment") {
                    var comment = new FeedbackComment();
                    comment.refreshFromJSON(feedbackItem);
                    feedback.addFeedbackItem(comment);
                }
            }
            annotateLines();

            var openLineNumber = $("#openLineNumber").html();
            if (openLineNumber) {
                var zeroBasedLineNumber = parseInt(openLineNumber) - 1;
                console.log("Opening to Line: ", openLineNumber);
                openLine(zeroBasedLineNumber);
                window.scrollTo(0, $("#comment-" + zeroBasedLineNumber).position().top - 100);
            }
        },
        error: function (jqXHR, status) {
            alert("Unable to retrieve information relating to the revision.");
        }
    });
});

