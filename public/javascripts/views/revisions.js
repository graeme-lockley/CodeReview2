var revision = undefined;

$(document).ready(function () {
    $(function () {
        $("#bob2").tablesorter({
            sortList: [
                [ 0, 0 ]
            ]
        });

        revision = new Revision({id: $("#revisionid").html()});
        revision.fetch({async: false});

        var BodyView = Backbone.View.extend({
            el: "#revisionDetail",
            initialize: function () {
                console.log("BodyView.initialise");
                this.listenTo(this.model, 'change', this.render);
            },
            render: function () {
                this.$el.html(_.template(templateOnName("revisions/_detail_view.html"), {revision: this.model}));

                this.revisionReviewView = new RevisionReviewView({model: this.model});
                this.revisionReviewView.render();

                this.revisionVerbs = new RevisionVerbs({model: this.model});
                this.revisionVerbs.render();

                return this;
            }
        });
        var RevisionVerbs = Backbone.View.extend({
            el: "#revision_verbs",
            render: function () {
                this.$el.html(_.template(templateOnName("revisions/_detail_verbs.html"), {revision: this.model}));
                return this;
            },
            events: {
                "click .verb": "clickButton"
            },
            clickButton: function (event) {
                this.model.verb($(event.target).html());
            }
        });
        var RevisionReviewView = Backbone.View.extend({
            el: "#review_detail",
            render: function () {
                this.$el.html(_.template(templateOnName("revisions/_review_view.html"), {revision: this.model}));
                return this;
            }
        });

        var bodyView = new BodyView({model: revision});
        bodyView.render();
    });
});