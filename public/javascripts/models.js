var loggedOnUser = undefined;

function templateOnName(name) {
    var templateResult = undefined;

    $.ajax({
        type: "GET",
        url: "/assets/html/" + name,
        async: false,
        success: function (data, status, jqXHR) {
            templateResult = data;
        },
        error: function (jqXHR, status) {
            console.error(jqXHR);
        }
    });

    return templateResult;
}

var Author = Backbone.Model.extend({
    urlRoot: "/authors",

    name: function() {
        return this.get("name");
    }
});

var Authors = Backbone.Collection.extend({
    model: Author,
    url: "/authors"
});

var Event = Backbone.Model.extend({
    urlRoot: "/events",

    author: function() {
        if (this.get("author") == undefined) {
            this.set("author", new Author({id: this.get("authorID")}));
            this.get("author").fetch({async: false});
        }
        return this.get("author");
    },
    stateFeedbackItem: function() {
        if (this.get("stateFeedbackItem") == undefined) {
            this.set("stateFeedbackItem", new FeedbackItem({id: this.get("state").feedbackID}));
            this.get("stateFeedbackItem").fetch({async: false});
        }
        return this.get("stateFeedbackItem");
    },
    stateRevision: function() {
        if (this.get("stateRevision") == undefined) {
            this.set("stateRevision", new Revision({id: this.get("state").revisionID}));
            this.get("stateRevision").fetch({async: false});
        }
        return this.get("stateRevision");
    }
});

var Events = Backbone.Collection.extend({
    model: Event,
    url: "/events"
});

var Revision = Backbone.Model.extend({
    urlRoot: "/revisions",

    reviewStatus: function () {
        return this.get("review").state;
    },
    reviewAuthor: function () {
        if (this.get("review").author == undefined) {
            return undefined;
        } else {
            var reviewAuthor = new Author({id: this.get("review").author.id});
            reviewAuthor.fetch({async: false});
            return reviewAuthor;
        }
    },
    reviewOutstanding: function () {
        return this.reviewStatus() == "Outstanding";
    },
    reviewInProgress: function () {
        return this.reviewStatus() == "In progress";
    },
    author: function () {
        if (this.get("author").author == undefined) {
            return this.get("author").name;
        } else {
            return this.get("author").author.name;
        }
    },
    repo: function () {
        return this.get("repo");
    },
    logMessage: function() {
        return this.get("logMessage");
    },
    revisionEntries: function () {
        if (this.get("revisionEntries") == undefined) {
            var entries = new RevisionEntries();
            entries.url = this.url() + "/revisionEntries";
            entries.fetch({async: false});
            this.set("revisionEntries", entries);
        }
        return this.get("revisionEntries");
    },
    status: function() {
        return this.revisionEntries().status();
    },
    number: function() {
        return this.get("number");
    },
    verbs: function () {
        return this.get("verbs");
    },
    verb: function (name, handler) {
        var revision = this;
        $.ajax({
            type: "GET",
            dataType: "json",
            url: this.verbs()[name],
            success: function (data, status, jqXHR) {
                revision.set(data);
            },
            failure: function (f) {
                console.log("Failure", f);
            },
            error: function (e, status) {
                alert("Error: " + e.responseText);
            }
        });
    }
});

var Revisions = Backbone.Collection.extend({
    model: Revision,
    comparator: function (r1, r2) {
        var r1date = r1.get("date");
        var r2date = r2.get("date");

        if (r1date < r2date) {
            return 1;
        } else if (r1date > r2date) {
            return -1;
        } else {
            return 0;
        }
    }
});

var RevisionEntry = Backbone.Model.extend({
    urlRoot: '/revisionEntries',
    showStuff: function () {
        console.log(this.attributes);
    },
    hasFeedback: function () {
        return this.get("feedback").length > 0;
    },
    feedback: function () {
        return new Feedback(this.get("feedback"), {revisionEntry: this});
    },
    entry: function() {
        return this.get("entry");
    },
    status: function() {
        return this.feedback().status();
    }
});

var RevisionEntries = Backbone.Collection.extend({
    model: RevisionEntry,

    status: function() {
        var status = "Closed";

        this.forEach(function(revisionEntry) {
            console.log("- ", revisionEntry.status());
            if (revisionEntry.status() == "Open") {
                status = "Open";
            }
        });

        console.log("RevisionEntries.status", status);

        return status;
    }
});

var FeedbackItem = Backbone.Model.extend({
    urlRoot: "/feedback",

    revisionEntry: function () {
        if (this.get("revisionEntry") == undefined) {
            this.set("revisionEntry", new RevisionEntry({id: this.get("revisionEntryID")}));
            this.get("revisionEntry").fetch({async: false});
        }
        return this.get("revisionEntry");
    },
    revision: function() {
        if (this.get("revision") == undefined) {
            this.set("revision", new Revision({id: this.get("revisionID")}));
            this.get("revision").fetch({async: false});
        }
        return this.get("revision");

    },
    comment: function () {
        return this.get("comment");
    },
    lineNumber: function () {
        return this.get("lineNumber") + 1;
    },
    isIssue: function () {
        return this.get("type") === "issue";
    },
    hasResponses: function () {
        return this.get("responses").length > 0;
    },
    responses: function () {
        return new FeedbackResponses(this.get("responses"));
    },
    status: function() {
        return this.get("status");
    }
});

var Feedback = Backbone.Collection.extend({
    model: FeedbackItem,

    initialize: function (models, options) {
        _.forEach(models, function (feedbackItem) {
            feedbackItem.revisionEntry = options.revisionEntry;
        });
        return this;
    },
    status: function() {
        var status = "Closed";

        this.forEach(function(feedbackItem) {
            if (feedbackItem.status() == "Open") {
                status = "Open";
            }
        });

        return status;
    }
});

var FeedbackResponse = Backbone.Model.extend({
    comment: function () {
        return this.get("comment");
    }
});

var FeedbackResponses = Backbone.Collection.extend({
    model: FeedbackResponse
});

function setHtml(domElement, templateName, templateState) {
    var theTemplate = $("#" + templateName).html();
    domElement.html(_.template(theTemplate, templateState));
}

function fetchLoggedOnUser(successContinuation) {
    $.ajax({
        type: "GET",
        url: "/auth/user",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            loggedOnUser = data;
            successContinuation();
        },
        error: function (jqXHR, status) {
            console.error(jqXHR);
        }
    });
}

$(document).ready(function () {
    fetchLoggedOnUser(function () {
    });
});
